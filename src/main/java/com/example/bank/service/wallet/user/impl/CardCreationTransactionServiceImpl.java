package com.example.bank.service.wallet.user.impl;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.wallet.user.CreateCardRequest;
import com.example.bank.entity.wallet.*;
import com.example.bank.enums.wallet.*;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.repository.wallet.*;
import com.example.bank.service.wallet.user.CardCreationTransactionService;
import com.example.bank.service.wallet.user.creation.CardCreationPolicy;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardCreationTransactionServiceImpl implements CardCreationTransactionService {
    private final CardCreationOrderRepository orders;
    private final WalletRepository wallets;
    private final UserRepository users;
    private final CardRepository legacyCards;
    private final InfiniCardRepository infiniCards;

    private static final Duration PROVIDER_TIMEOUT = Duration.ofMinutes(30);
    // Release a missing card only long after the apply request was sent.
    private static final Duration NOT_FOUND_GRACE = Duration.ofMinutes(60);

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Reservation reserve(CreateCardRequest request, Long userId, String tokenType, String applicantEmail, Integer productId) {
        Wallet wallet = wallets.findByUserIdAndCurrencyForUpdate(userId, Stablecoin.USD)
                .orElseThrow(() -> error(MessageKeys.WALLET_NOT_FOUND));
        String id = request.getRequestId().toString();
        var existing = orders.findById(id);
        if (existing.isPresent()) {
            CardCreationOrder old = existing.get();
            if (!old.getUserId().equals(userId) ||
                    old.getAmount().compareTo(request.getAmount()) != 0 ||
                    !old.getName().equals(request.getName().trim()) ||
                    !old.getHolderName().equals(request.getHolderName().trim()) ||
                    !Objects.equals(old.getNote(), request.getNote())) {
                throw new WalletException(MessageKeys.CARD_CREATION_REQUEST_CONFLICT, HttpStatus.CONFLICT);
            }
            return new Reservation(old, false);
        }
        if (!"ACTIVE".equals(wallet.getStatus()) || Boolean.TRUE.equals(wallet.getIsLocked()) ||
                wallet.getDeletedAt() != null) {
            throw error(MessageKeys.CARD_CREATION_WALLET_UNAVAILABLE);
        }
        BigDecimal total = CardCreationPolicy.totalDebit(request.getAmount());
        if (wallet.getAvailableBalance().compareTo(total) < 0) {
            throw error(MessageKeys.CARD_CREATION_INSUFFICIENT_BALANCE);
        }
        var user = users.findById(userId).orElseThrow(() -> error(MessageKeys.USER_NOT_FOUND));
        var pendingOrder = orders.findFirstByUserIdAndStatusInOrderByCreatedAtAsc(
                userId, List.of("PENDING", "REVIEW_REQUIRED"));
        if (pendingOrder != null && pendingOrder.isPresent()) {
            // Lets the client follow the existing request instead of starting a new one.
            throw new WalletException(MessageKeys.CARD_CREATION_ALREADY_PENDING, HttpStatus.CONFLICT,
                    Map.of("request_id", pendingOrder.get().getRequestId()));
        }
        if (user.getCardOpenLimit() == null ||
                legacyCards.countByUserId(userId) + infiniCards.countByUserId(userId) >= user.getCardOpenLimit()) {
            throw error(MessageKeys.CARD_LIMIT_EXCEEDED);
        }
        CardCreationOrder order = new CardCreationOrder();
        order.setRequestId(id);
        order.setUserId(userId);
        order.setWalletId(wallet.getId());
        order.setProductId(productId);
        order.setName(request.getName().trim());
        order.setHolderName(request.getHolderName().trim());
        order.setApplicantEmail(applicantEmail.trim());
        order.setNote(request.getNote());
        order.setTokenType(tokenType);
        order.setAmount(request.getAmount());
        order.setFee(CardCreationPolicy.FEE);
        order.setTotalDebit(total);
        order.setStatus("PENDING");
        order.setNextCheckAt(Instant.now());
        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(total));
        wallet.setFrozenBalance(wallet.getFrozenBalance().add(total));
        orders.saveAndFlush(order); // Durable before the external request.
        return new Reservation(order, true);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markSubmitting(String id) {
        CardCreationOrder order = locked(id);
        if (!"PENDING".equals(order.getStatus()) || order.getProviderCardId() != null) return false;
        order.setApplyStartedAt(Instant.now());
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void accepted(String id, JsonNode result) {
        CardCreationOrder order = locked(id);
        if (!pending(order)) return;
        String providerId = result.path("id").asText("");
        if (providerId.isBlank() || providerId.length() > 100) {
            throw new IllegalStateException("Missing Infini card ID");
        }
        if (order.getProviderCardId() != null && !order.getProviderCardId().equals(providerId)) {
            throw new IllegalStateException("Conflicting Infini card ID");
        }
        order.setProviderCardId(providerId);
        order.setProviderFee(decimal(result, "total_fee"));
        order.setProviderTotalPay(decimal(result, "total_pay_amount"));
        BigDecimal topUp = decimal(result, "total_top_up_amount");
        if (topUp != null && topUp.compareTo(order.getAmount()) != 0 ||
                order.getProviderTotalPay() != null &&
                order.getProviderTotalPay().compareTo(order.getAmount().add(CardCreationPolicy.FEE)) > 0) {
            // The provider charges more than the customer pays; flag it, never log amounts or payloads.
            log.warn("Provider cost differs from customer price requestId={}", id);
        }
        order.setNextCheckAt(Instant.now());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void review(String id, String reason) {
        CardCreationOrder order = locked(id);
        if (!pending(order)) return;
        order.setStatus("REVIEW_REQUIRED");
        order.setReviewReason(reason);
        order.setNextCheckAt(Instant.now().plusSeconds(60));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rejected(String id, String reason) {
        CardCreationOrder order = locked(id);
        if (!pending(order) || order.getProviderCardId() != null) return;
        release(order, reason);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expireUnsent(String id) {
        CardCreationOrder order = locked(id);
        // applyStartedAt != null means the request may have reached the provider.
        if (!"PENDING".equals(order.getStatus()) || order.getProviderCardId() != null ||
                order.getApplyStartedAt() != null) return;
        release(order, "APPLY_NOT_SENT");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void providerLookup(String id, List<String> providerCardIds) {
        CardCreationOrder order = locked(id);
        if (!pending(order) || order.getProviderCardId() != null || order.getApplyStartedAt() == null) return;
        Instant now = Instant.now();
        order.setNextCheckAt(now.plusSeconds(delaySeconds(age(order, now))));
        if (providerCardIds.size() == 1) {
            // Found: continue with the normal status flow.
            order.setProviderCardId(providerCardIds.get(0));
            order.setStatus("PENDING");
            order.setReviewReason(null);
            order.setNextCheckAt(now);
        } else if (providerCardIds.size() > 1) {
            order.setStatus("REVIEW_REQUIRED");
            order.setReviewReason("PROVIDER_LOOKUP_AMBIGUOUS");
        } else if (Duration.between(order.getApplyStartedAt(), now).compareTo(NOT_FOUND_GRACE) > 0) {
            // Listed successfully and no card carries this alias long after the request was sent.
            release(order, "PROVIDER_CARD_NOT_FOUND");
        } else if ("PENDING".equals(order.getStatus())) {
            order.setStatus("REVIEW_REQUIRED");
            order.setReviewReason("APPLY_RESULT_UNKNOWN");
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void statusUnavailable(String id) {
        CardCreationOrder order = locked(id);
        if (!pending(order)) return;
        Instant now = Instant.now();
        Duration age = age(order, now);
        order.setNextCheckAt(now.plusSeconds(delaySeconds(age)));
        // Short outages keep the order PENDING; only a long one asks for attention.
        if ("PENDING".equals(order.getStatus()) && age.compareTo(PROVIDER_TIMEOUT) > 0) {
            order.setStatus("REVIEW_REQUIRED");
            order.setReviewReason("STATUS_UNAVAILABLE");
        }
    }

    private static Duration age(CardCreationOrder order, Instant now) {
        return Duration.between(order.getCreatedAt() == null ? now : order.getCreatedAt(), now);
    }

    // Poll quickly at first, then back off.
    private static long delaySeconds(Duration age) {
        return age.compareTo(Duration.ofMinutes(5)) < 0 ? 15
                : age.compareTo(Duration.ofMinutes(30)) < 0 ? 60 : 300;
    }

    // Moves the held amount back to available. Nothing was charged yet; the fee is only taken on success.
    private void release(CardCreationOrder order, String reason) {
        Wallet wallet = wallet(order);
        wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(order.getTotalDebit()));
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(order.getTotalDebit()));
        order.setStatus("FAILED");
        order.setReviewReason(reason);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void synchronize(String id, JsonNode snapshot) {
        CardCreationOrder order = locked(id);
        if (!pending(order)) return;
        Instant now = Instant.now();
        Duration age = age(order, now);
        order.setNextCheckAt(now.plusSeconds(delaySeconds(age)));
        if (!Objects.equals(order.getProviderCardId(), snapshot.path("id").asText())) {
            throw new IllegalStateException("Infini status ID mismatch");
        }
        String status = snapshot.path("status").asText().toLowerCase(Locale.ROOT);
        if ("init".equals(status) || "pending".equals(status)) {
            if ("PENDING".equals(order.getStatus()) && age.compareTo(PROVIDER_TIMEOUT) > 0) {
                order.setStatus("REVIEW_REQUIRED");
                order.setReviewReason("PROVIDER_TIMEOUT");
            }
            return;
        }
        // The docs list no failure status (only init, pending, active, pending_delete, deleted),
        // so anything other than active is kept for review and the funds stay held.
        if (!"active".equals(status) || !"USD".equals(snapshot.path("currency").asText())) {
            order.setStatus("REVIEW_REQUIRED");
            order.setReviewReason("UNEXPECTED_PROVIDER_STATUS");
            return;
        }
        BigDecimal providerBalance = decimal(snapshot, "available_balance");
        if (providerBalance == null || providerBalance.compareTo(order.getAmount()) != 0) {
            order.setStatus("REVIEW_REQUIRED");
            order.setReviewReason("INITIAL_BALANCE_MISMATCH");
            return;
        }
        Wallet wallet = wallet(order);
        InfiniCard card = InfiniCard.builder()
                .userId(order.getUserId())
                .infiniCardId(order.getProviderCardId()).productId(order.getProductId())
                .tokenType(order.getTokenType()).creationFee(order.getFee())
                .holderName(order.getHolderName()).holderEmail(order.getApplicantEmail())
                .name(order.getName()).note(order.getNote()).currency("USD")
                .initialAmount(order.getAmount()).remainingAmount(order.getAmount())
                .spentAmount(BigDecimal.ZERO).lockedAmount(BigDecimal.ZERO)
                .status(CardStatus.ACTIVE).build();
        String mask = snapshot.path("mask").asText("");
        if (mask.length() >= 4) card.setLast4(mask.substring(mask.length() - 4));
        // Do not invent a BIN/product mapping or store PAN/CVV.
        infiniCards.saveAndFlush(card);
        wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(order.getTotalDebit()));
        wallet.setAllocatedBalance(wallet.getAllocatedBalance().add(order.getAmount()));
        wallet.setTotalBalance(wallet.getTotalBalance().subtract(order.getFee()));
        order.setCardId(card.getId());
        order.setStatus("SUCCEEDED");
        order.setReviewReason(null);
    }

    private CardCreationOrder locked(String id) {
        return orders.findForUpdate(id).orElseThrow();
    }
    private Wallet wallet(CardCreationOrder order) {
        Wallet wallet = wallets.findByUserIdAndCurrencyForUpdate(order.getUserId(), Stablecoin.USD).orElseThrow();
        if (!wallet.getId().equals(order.getWalletId()) ||
                wallet.getFrozenBalance().compareTo(order.getTotalDebit()) < 0) {
            throw new IllegalStateException("Creation reservation mismatch");
        }
        return wallet;
    }
    private boolean pending(CardCreationOrder order) {
        return "PENDING".equals(order.getStatus()) || "REVIEW_REQUIRED".equals(order.getStatus());
    }
    private BigDecimal decimal(JsonNode node, String field) {
        return node.hasNonNull(field) ? new BigDecimal(node.get(field).asText()) : null;
    }
    private WalletException error(String key) {
        return new WalletException(key, HttpStatus.BAD_REQUEST);
    }
}
