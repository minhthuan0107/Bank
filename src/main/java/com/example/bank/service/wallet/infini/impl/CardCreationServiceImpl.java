package com.example.bank.service.wallet.infini.impl;

import com.example.bank.common.config.infini.*;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.wallet.infini.CreateCardRequest;
import com.example.bank.dto.response.wallet.user.*;
import com.example.bank.entity.wallet.infini.CardCreationOrder;
import com.example.bank.enums.wallet.Stablecoin;
import com.example.bank.repository.wallet.*;
import com.example.bank.service.wallet.infini.CardCreationService;
import com.example.bank.service.wallet.infini.CardCreationTransactionService;
import com.example.bank.service.wallet.infini.CardCreationPolicy;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.example.bank.dto.response.wallet.infini.CardCreationConfigResponse;
import com.example.bank.repository.wallet.infini.CardCreationOrderRepository;
import com.example.bank.dto.response.wallet.infini.CardCreationResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardCreationServiceImpl implements CardCreationService {
    private final InfiniProperties properties;
    private final InfiniCardClient client;
    private final CardCreationTransactionService ledger;
    private final CardCreationOrderRepository orders;
    private final WalletRepository wallets;

    // Longer than the apply timeout, so a live request is never expired by the scheduler.
    private static final Duration UNSUBMITTED_GRACE = Duration.ofMinutes(2);

    @Override
    public CardCreationResponse createCard(CreateCardRequest request, Long userId) {
        client.validateConfiguration(); // Reject missing keys before holding customer funds.
        CardCreationPolicy.totalDebit(request.getAmount());
        var reservation = ledger.reserve(request, userId, properties.getTokenType(), properties.getApplicantEmail(), properties.getProductId());
        String id = reservation.order().getRequestId();
        // The order may have been expired by the scheduler meanwhile; never send in that case.
        if (reservation.submit() && ledger.markSubmitting(id)) {
            try {
                ledger.accepted(id, client.apply(reservation.order()));
            } catch (InfiniCardClient.DefiniteRejection e) {
                ledger.rejected(id, e.getReason());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ledger.review(id, "INTERRUPTED_RESULT_UNKNOWN");
            } catch (Exception e) {
                // Never log payload, member email, keys or vendor response bodies.
                log.warn("Card application requires reconciliation requestId={} reason={}", id,
                        e.getClass().getSimpleName());
                ledger.review(id, "APPLY_RESULT_UNKNOWN");
            }
            // The status check is done by the scheduler so this request returns quickly.
        }
        return getCreationStatus(UUID.fromString(id), userId);
    }

    @Override
    public CardCreationResponse getCreationStatus(UUID id, Long userId) {
        return CardCreationResponse.from(orders.findByRequestIdAndUserId(id.toString(), userId)
                .orElseThrow(() -> new WalletException(MessageKeys.CARD_CREATION_NOT_FOUND, HttpStatus.NOT_FOUND)));
    }

    @Override
    public CardCreationConfigResponse getCreationConfig(Long userId) {
        var wallet = wallets.findByUserIdAndCurrency(userId, Stablecoin.USD)
                .orElseThrow(() -> new WalletException(MessageKeys.WALLET_NOT_FOUND, HttpStatus.NOT_FOUND));
        var max = CardCreationPolicy.maxAmount(wallet.getAvailableBalance());
        boolean hasPendingOrder = orders.existsByUserIdAndStatusIn(userId, List.of("PENDING", "REVIEW_REQUIRED"));
        boolean canCreate = properties.isEnabled() && !hasPendingOrder && "ACTIVE".equals(wallet.getStatus()) &&
                !Boolean.TRUE.equals(wallet.getIsLocked()) &&
                max.compareTo(CardCreationPolicy.MIN_AMOUNT) >= 0;
        return CardCreationConfigResponse.builder()
                .minAmount(CardCreationPolicy.MIN_AMOUNT)
                .creationFee(CardCreationPolicy.FEE)
                .availableBalance(wallet.getAvailableBalance())
                .maxAmount(max)
                .canCreate(canCreate)
                .productIds(java.util.Set.of(properties.getProductId()))
                .build();
    }

    // Read-only provider calls can be retried. Applications are never automatically resubmitted.
    @Override
    public void reconcilePendingOrders() {
        if (!properties.isEnabled()) return;
        for (String id : orders.findDue(Instant.now(), PageRequest.of(0, 25))) {
            reconcile(id);
            if (Thread.currentThread().isInterrupted()) return;
        }
        Instant now = Instant.now();
        // The apply request was never sent: the funds can be released safely.
        for (String id : orders.findUnsent(now.minus(UNSUBMITTED_GRACE), PageRequest.of(0, 25))) {
            try {
                ledger.expireUnsent(id);
            } catch (Exception e) {
                log.warn("Unsent order check failed requestId={} reason={}", id, e.getClass().getSimpleName());
            }
        }
        // The request was sent but no card ID was stored: find the card by its alias (the request ID).
        for (String id : orders.findAwaitingProviderId(now.minus(UNSUBMITTED_GRACE), now, PageRequest.of(0, 25))) {
            lookupProviderCard(id);
            if (Thread.currentThread().isInterrupted()) return;
        }
    }

    private void lookupProviderCard(String id) {
        try {
            var cards = client.findByAlias(id);
            ledger.providerLookup(id, cards.stream().map(c -> c.path("id").asText()).toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("Provider lookup failed requestId={} reason={}", id, e.getClass().getSimpleName());
            ledger.statusUnavailable(id);
        }
    }

    @Override
    public void reconcile(String id) {
        CardCreationOrder order = orders.findById(id).orElseThrow();
        if (order.getProviderCardId() == null ||
                !("PENDING".equals(order.getStatus()) || "REVIEW_REQUIRED".equals(order.getStatus()))) return;
        try {
            ledger.synchronize(id, client.status(order.getProviderCardId()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Temporary failure: keep the order and try again; it never moves money.
            log.warn("Card status check failed requestId={} reason={}", id, e.getClass().getSimpleName());
            ledger.statusUnavailable(id);
        }
    }
}
