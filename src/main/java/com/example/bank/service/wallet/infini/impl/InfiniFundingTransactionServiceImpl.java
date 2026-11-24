package com.example.bank.service.wallet.infini.impl;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.wallet.infini.InfiniFundingRequest;
import com.example.bank.entity.wallet.infini.InfiniCard;
import com.example.bank.entity.wallet.infini.InfiniFundingOrder;
import com.example.bank.entity.wallet.Wallet;
import com.example.bank.enums.wallet.CardStatus;
import com.example.bank.enums.wallet.Stablecoin;
import com.example.bank.repository.wallet.infini.InfiniCardRepository;
import com.example.bank.repository.wallet.infini.InfiniFundingOrderRepository;
import com.example.bank.repository.wallet.WalletRepository;
import com.example.bank.service.wallet.infini.InfiniFundingTransactionService;
import com.example.bank.service.wallet.infini.InfiniFundingPolicy;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.example.bank.entity.wallet.infini.InfiniFundingOrder.*;

/**
 * Wallet effects (total never changes; no fee):
 * top-up  hold:    available -= a, frozen += a
 * top-up  settle:  frozen -= a, allocated += a, card += a
 * top-up  release: frozen -= a, available += a
 * redeem  settle:  allocated -= a, available += a, card -= a
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InfiniFundingTransactionServiceImpl implements InfiniFundingTransactionService {

    private final InfiniFundingOrderRepository orders;
    private final WalletRepository wallets;
    private final InfiniCardRepository cards;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Reservation reserve(String type, InfiniFundingRequest request, Long userId, Long cardId,
                               String tokenType, BigDecimal providerCardBalance) {
        InfiniFundingPolicy.validate(type, request.getAmount());
        Wallet wallet = wallets.findByUserIdAndCurrencyForUpdate(userId, Stablecoin.USD)
                .orElseThrow(() -> new WalletException(MessageKeys.WALLET_NOT_FOUND, HttpStatus.NOT_FOUND));

        String id = request.getRequestId().toString();
        var existing = orders.findById(id);
        if (existing.isPresent()) {
            InfiniFundingOrder old = existing.get();
            if (!old.getUserId().equals(userId) || !old.getCardId().equals(cardId) ||
                    !old.getType().equals(type) || old.getAmount().compareTo(request.getAmount()) != 0) {
                throw new WalletException(MessageKeys.INFINI_FUNDING_REQUEST_CONFLICT, HttpStatus.CONFLICT);
            }
            return new Reservation(old, false);
        }

        InfiniCard card = cards.findByIdAndUserIdForUpdate(cardId, userId)
                .orElseThrow(() -> new WalletException(MessageKeys.CARD_NOT_FOUND, HttpStatus.NOT_FOUND));
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new WalletException(MessageKeys.INFINI_FUNDING_CARD_NOT_ACTIVE, HttpStatus.CONFLICT);
        }
        if (!"ACTIVE".equals(wallet.getStatus()) || Boolean.TRUE.equals(wallet.getIsLocked()) ||
                wallet.getDeletedAt() != null) {
            throw new WalletException(MessageKeys.INFINI_FUNDING_WALLET_UNAVAILABLE, HttpStatus.BAD_REQUEST);
        }
        // One unresolved order per card, so a provider event can be matched to it without ambiguity.
        var pending = orders.findFirstByCardIdAndStatusInOrderByCreatedAtAsc(cardId, List.of(PENDING, REVIEW_REQUIRED));
        if (pending != null && pending.isPresent()) {
            throw new WalletException(MessageKeys.INFINI_FUNDING_ALREADY_PENDING, HttpStatus.CONFLICT,
                    Map.of("request_id", pending.get().getRequestId()));
        }

        BigDecimal amount = request.getAmount();
        if (TOP_UP.equals(type)) {
            if (wallet.getAvailableBalance().compareTo(amount) < 0) {
                throw new WalletException(MessageKeys.INSUFFICIENT_BALANCE, HttpStatus.BAD_REQUEST);
            }
            wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
            wallet.setFrozenBalance(wallet.getFrozenBalance().add(amount));
        } else {
            // The real balance comes from Infini: local card balances do not track spending yet.
            requireWithdrawCooldownOver(cardId);
            InfiniFundingPolicy.validateWithdrawal(providerCardBalance, amount);
            if (wallet.getAllocatedBalance().compareTo(amount) < 0) {
                throw new WalletException(MessageKeys.INSUFFICIENT_CARD_BALANCE, HttpStatus.BAD_REQUEST);
            }
        }

        InfiniFundingOrder order = new InfiniFundingOrder();
        order.setRequestId(id);
        order.setUserId(userId);
        order.setWalletId(wallet.getId());
        order.setCardId(card.getId());
        order.setProviderCardId(card.getInfiniCardId());
        order.setType(type);
        order.setAmount(amount);
        order.setTokenType(tokenType);
        order.setStatus(PENDING);
        orders.saveAndFlush(order); // Durable before the external request.
        return new Reservation(order, true);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markSubmitting(String id) {
        InfiniFundingOrder order = locked(id);
        if (!PENDING.equals(order.getStatus()) || order.getSubmitStartedAt() != null) return false;
        order.setSubmitStartedAt(Instant.now());
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void succeeded(String id, JsonNode result) {
        InfiniFundingOrder order = locked(id);
        if (!order.isUnresolved()) return;
        String txId = result.path("tx_id").asText("");
        if (txId.isBlank() || txId.length() > 100) {
            throw new IllegalStateException("Missing Infini transaction ID");
        }
        if (result.hasNonNull("card_balance")) {
            order.setProviderCardBalance(new BigDecimal(result.get("card_balance").asText()));
        }
        settle(order, txId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rejected(String id, String reason) {
        InfiniFundingOrder order = locked(id);
        if (!order.isUnresolved() || order.getProviderTxId() != null) return;
        release(order, reason);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void review(String id, String reason) {
        InfiniFundingOrder order = locked(id);
        if (!order.isUnresolved()) return;
        order.setStatus(REVIEW_REQUIRED);
        order.setReviewReason(reason);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expireUnsent(String id) {
        InfiniFundingOrder order = locked(id);
        if (!PENDING.equals(order.getStatus()) || order.getSubmitStartedAt() != null) return;
        release(order, "SUBMIT_NOT_SENT");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reviewInterrupted(String id) {
        InfiniFundingOrder order = locked(id);
        if (!PENDING.equals(order.getStatus()) || order.getSubmitStartedAt() == null ||
                order.getProviderTxId() != null) return;
        order.setStatus(REVIEW_REQUIRED);
        order.setReviewReason("SUBMIT_INTERRUPTED");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resolveFromProvider(String id, String providerTxId, boolean completed) {
        InfiniFundingOrder order = locked(id);
        if (!order.isUnresolved() || order.getProviderTxId() != null || order.getSubmitStartedAt() == null) return;
        if (completed) {
            settle(order, providerTxId);
        } else {
            release(order, "PROVIDER_FAILED");
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void flagProviderFailure(String id) {
        InfiniFundingOrder order = locked(id);
        if (!SUCCEEDED.equals(order.getStatus())) return;
        // Funds were already moved; an operator must reconcile with Infini.
        order.setReviewReason("PROVIDER_REPORTED_FAILED");
        log.error("Infini reported failure for a settled funding order requestId={}", id);
    }

    private void settle(InfiniFundingOrder order, String txId) {
        Wallet wallet = wallet(order);
        InfiniCard card = cards.findByIdForUpdate(order.getCardId()).orElseThrow();
        BigDecimal amount = order.getAmount();
        if (TOP_UP.equals(order.getType())) {
            wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(amount));
            wallet.setAllocatedBalance(wallet.getAllocatedBalance().add(amount));
            card.setRemainingAmount(card.getRemainingAmount().add(amount));
        } else {
            wallet.setAllocatedBalance(wallet.getAllocatedBalance().subtract(amount));
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
            card.setRemainingAmount(card.getRemainingAmount().subtract(amount).max(BigDecimal.ZERO));
        }
        order.setProviderTxId(txId);
        order.setStatus(SUCCEEDED);
        order.setReviewReason(null);
    }

    private void release(InfiniFundingOrder order, String reason) {
        if (TOP_UP.equals(order.getType())) {
            Wallet wallet = wallet(order);
            wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(order.getAmount()));
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(order.getAmount()));
        }
        // A withdrawal holds nothing in the wallet, so there is nothing to release.
        order.setStatus(FAILED);
        order.setReviewReason(reason);
    }

    private Wallet wallet(InfiniFundingOrder order) {
        Wallet wallet = wallets.findByUserIdAndCurrencyForUpdate(order.getUserId(), Stablecoin.USD).orElseThrow();
        boolean holdMissing = TOP_UP.equals(order.getType()) &&
                wallet.getFrozenBalance().compareTo(order.getAmount()) < 0;
        boolean allocatedMissing = REDEEM.equals(order.getType()) &&
                wallet.getAllocatedBalance().compareTo(order.getAmount()) < 0;
        if (!wallet.getId().equals(order.getWalletId()) || holdMissing || allocatedMissing) {
            throw new IllegalStateException("Funding reservation mismatch");
        }
        return wallet;
    }

    private void requireWithdrawCooldownOver(Long cardId) {
        var lastTopUp = orders.findFirstByCardIdAndTypeAndStatusOrderBySubmitStartedAtDesc(cardId, TOP_UP, SUCCEEDED);
        if (lastTopUp == null || lastTopUp.isEmpty() || lastTopUp.get().getSubmitStartedAt() == null) return;
        Instant availableAt = lastTopUp.get().getSubmitStartedAt().plus(InfiniFundingPolicy.WITHDRAW_COOLDOWN);
        if (Instant.now().isBefore(availableAt)) {
            throw new WalletException(MessageKeys.INFINI_FUNDING_WITHDRAW_COOLDOWN, HttpStatus.CONFLICT,
                    Map.of("available_at", availableAt.toString()));
        }
    }

    private InfiniFundingOrder locked(String id) {
        return orders.findForUpdate(id).orElseThrow();
    }
}
