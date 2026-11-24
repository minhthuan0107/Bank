package com.example.bank.service.wallet.infini.impl;

import com.example.bank.common.config.infini.InfiniCardClient;
import com.example.bank.common.config.infini.InfiniProperties;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.wallet.infini.InfiniFundingRequest;
import com.example.bank.dto.response.wallet.infini.InfiniFundingResponse;
import com.example.bank.entity.wallet.infini.InfiniCard;
import com.example.bank.entity.wallet.infini.InfiniFundingOrder;
import com.example.bank.enums.wallet.CardStatus;
import com.example.bank.repository.wallet.infini.InfiniCardRepository;
import com.example.bank.repository.wallet.infini.InfiniFundingOrderRepository;
import com.example.bank.service.wallet.infini.InfiniFundingService;
import com.example.bank.service.wallet.infini.InfiniFundingTransactionService;
import com.example.bank.service.wallet.infini.InfiniFundingPolicy;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static com.example.bank.entity.wallet.infini.InfiniFundingOrder.REDEEM;
import static com.example.bank.entity.wallet.infini.InfiniFundingOrder.TOP_UP;

@Service
@RequiredArgsConstructor
@Slf4j
public class InfiniFundingServiceImpl implements InfiniFundingService {

    // Longer than the request timeout, so a live request is never expired by the scheduler.
    private static final Duration STALE_AFTER = Duration.ofMinutes(2);

    private final InfiniProperties properties;
    private final InfiniCardClient client;
    private final InfiniFundingTransactionService ledger;
    private final InfiniFundingOrderRepository orders;
    private final InfiniCardRepository cards;

    @Override
    public InfiniFundingResponse topUp(Long userId, Long cardId, InfiniFundingRequest request) {
        client.validateConfiguration();
        InfiniFundingPolicy.validate(TOP_UP, request.getAmount());
        var reservation = ledger.reserve(TOP_UP, request, userId, cardId, properties.getTokenType(), null);
        return submit(reservation, userId);
    }

    @Override
    public InfiniFundingResponse withdraw(Long userId, Long cardId, InfiniFundingRequest request) {
        client.validateConfiguration();
        InfiniFundingPolicy.validate(REDEEM, request.getAmount());
        BigDecimal providerBalance = null;
        // A retry of a stored request needs no new balance check.
        if (!orders.existsById(request.getRequestId().toString())) {
            providerBalance = providerBalance(userId, cardId);
        }
        var reservation = ledger.reserve(REDEEM, request, userId, cardId, properties.getTokenType(), providerBalance);
        return submit(reservation, userId);
    }

    private InfiniFundingResponse submit(InfiniFundingTransactionService.Reservation reservation, Long userId) {
        InfiniFundingOrder order = reservation.order();
        String id = order.getRequestId();
        // The order may have been expired by the scheduler meanwhile; never send in that case.
        if (reservation.submit() && ledger.markSubmitting(id)) {
            try {
                JsonNode result = TOP_UP.equals(order.getType()) ? client.topUp(order) : client.redeem(order);
                ledger.succeeded(id, result);
            } catch (InfiniCardClient.DefiniteRejection e) {
                ledger.rejected(id, e.getReason());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ledger.review(id, "INTERRUPTED_RESULT_UNKNOWN");
            } catch (Exception e) {
                // Never log payloads, keys or vendor response bodies.
                log.warn("Card funding requires reconciliation requestId={} reason={}", id,
                        e.getClass().getSimpleName());
                ledger.review(id, "SUBMIT_RESULT_UNKNOWN");
            }
        }
        return getStatus(UUID.fromString(id), userId);
    }

    // Reads the real card balance from Infini; local balances do not track spending yet.
    private BigDecimal providerBalance(Long userId, Long cardId) {
        InfiniCard card = cards.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new WalletException(MessageKeys.CARD_NOT_FOUND, HttpStatus.NOT_FOUND));
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new WalletException(MessageKeys.INFINI_FUNDING_CARD_NOT_ACTIVE, HttpStatus.CONFLICT);
        }
        try {
            JsonNode snapshot = client.status(card.getInfiniCardId());
            if (!card.getInfiniCardId().equals(snapshot.path("id").asText()) ||
                    !"active".equalsIgnoreCase(snapshot.path("status").asText()) ||
                    !snapshot.hasNonNull("available_balance")) {
                throw new WalletException(MessageKeys.INFINI_FUNDING_CARD_NOT_ACTIVE, HttpStatus.CONFLICT);
            }
            return new BigDecimal(snapshot.get("available_balance").asText());
        } catch (WalletException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WalletException(MessageKeys.INFINI_FUNDING_PROVIDER_UNAVAILABLE, HttpStatus.BAD_GATEWAY);
        } catch (Exception e) {
            // Nothing is held yet, so failing here is safe.
            log.warn("Card balance check failed cardId={} reason={}", cardId, e.getClass().getSimpleName());
            throw new WalletException(MessageKeys.INFINI_FUNDING_PROVIDER_UNAVAILABLE, HttpStatus.BAD_GATEWAY);
        }
    }

    @Override
    public InfiniFundingResponse getStatus(UUID requestId, Long userId) {
        return InfiniFundingResponse.from(orders.findByRequestIdAndUserId(requestId.toString(), userId)
                .orElseThrow(() -> new WalletException(MessageKeys.INFINI_FUNDING_NOT_FOUND, HttpStatus.NOT_FOUND)));
    }

    @Override
    public void expireStaleOrders() {
        if (!properties.isEnabled()) return;
        Instant before = Instant.now().minus(STALE_AFTER);
        for (String id : orders.findUnsent(before, PageRequest.of(0, 25))) {
            try {
                ledger.expireUnsent(id);
            } catch (Exception e) {
                log.warn("Unsent funding order check failed requestId={} reason={}", id, e.getClass().getSimpleName());
            }
        }
        // Sent but never recorded: keep any hold and wait for the provider webhook.
        for (String id : orders.findInterrupted(before, PageRequest.of(0, 25))) {
            try {
                ledger.reviewInterrupted(id);
            } catch (Exception e) {
                log.warn("Interrupted funding order check failed requestId={} reason={}", id, e.getClass().getSimpleName());
            }
        }
    }
}
