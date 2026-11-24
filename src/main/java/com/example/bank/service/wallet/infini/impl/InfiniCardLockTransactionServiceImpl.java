package com.example.bank.service.wallet.infini.impl;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.entity.wallet.Wallet;
import com.example.bank.entity.wallet.infini.InfiniCard;
import com.example.bank.enums.wallet.CardLockedBy;
import com.example.bank.enums.wallet.CardStatus;
import com.example.bank.enums.wallet.Stablecoin;
import com.example.bank.repository.wallet.WalletRepository;
import com.example.bank.repository.wallet.infini.InfiniCardRepository;
import com.example.bank.repository.wallet.infini.InfiniFundingOrderRepository;
import com.example.bank.service.wallet.infini.InfiniCardLockTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.example.bank.entity.wallet.infini.InfiniFundingOrder.PENDING;
import static com.example.bank.entity.wallet.infini.InfiniFundingOrder.REVIEW_REQUIRED;

/**
 * lock:   allocated -= b, frozen += b, card.locked = b, card.remaining = 0
 * unlock: frozen -= b, allocated += b, card.remaining = b, card.locked = 0
 * total never changes.
 */
@Service
@RequiredArgsConstructor
public class InfiniCardLockTransactionServiceImpl implements InfiniCardLockTransactionService {

    private final WalletRepository wallets;
    private final InfiniCardRepository cards;
    private final InfiniFundingOrderRepository fundingOrders;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BlockResult block(Long userId, Long cardId, CardLockedBy actor) {
        Wallet wallet = wallets.findByUserIdAndCurrencyForUpdate(userId, Stablecoin.USD)
                .orElseThrow(() -> new WalletException(MessageKeys.WALLET_NOT_FOUND, HttpStatus.NOT_FOUND));
        InfiniCard card = cards.findByIdAndUserIdForUpdate(cardId, userId)
                .orElseThrow(() -> new WalletException(MessageKeys.CARD_NOT_FOUND, HttpStatus.NOT_FOUND));
        if (card.getStatus() == CardStatus.BLOCKED) {
            if (actor == CardLockedBy.ADMIN && card.getLockedBy() != CardLockedBy.ADMIN) {
                card.setLockedBy(CardLockedBy.ADMIN);
                return new BlockResult(card, false);
            }
            throw new WalletException(MessageKeys.CARD_ALREADY_BLOCKED, HttpStatus.CONFLICT);
        }
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new WalletException(MessageKeys.INFINI_FUNDING_CARD_NOT_ACTIVE, HttpStatus.CONFLICT);
        }
        // A top-up/withdrawal still settling would change the balance being moved.
        var pending = fundingOrders.findFirstByCardIdAndStatusInOrderByCreatedAtAsc(cardId, List.of(PENDING, REVIEW_REQUIRED));
        if (pending != null && pending.isPresent()) {
            throw new WalletException(MessageKeys.CARD_HAS_PENDING_TRANSACTION, HttpStatus.CONFLICT);
        }
        BigDecimal amount = card.getRemainingAmount().max(BigDecimal.ZERO);
        if (wallet.getAllocatedBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Allocated balance lower than card balance");
        }
        wallet.setAllocatedBalance(wallet.getAllocatedBalance().subtract(amount));
        wallet.setFrozenBalance(wallet.getFrozenBalance().add(amount));
        card.setLockedAmount(amount);
        card.setRemainingAmount(BigDecimal.ZERO);
        card.setStatus(CardStatus.BLOCKED);
        card.setLockedBy(actor);
        return new BlockResult(card, true);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UnblockResult unblock(Long cardId, CardLockedBy actor) {
        // Only the owner ID is read here: loading the entity would hide concurrent changes from the locked read.
        Long ownerId = cards.findOwnerId(cardId).orElseThrow();
        // Same lock order as block(): wallet first, then card.
        Wallet wallet = wallets.findByUserIdAndCurrencyForUpdate(ownerId, Stablecoin.USD).orElseThrow();
        InfiniCard card = cards.findByIdForUpdate(cardId).orElseThrow();
        if (card.getStatus() != CardStatus.BLOCKED) return UnblockResult.NOT_BLOCKED;
        if (!actor.mayRelease(card.getLockedBy())) return UnblockResult.NOT_ALLOWED;
        BigDecimal amount = card.getLockedAmount() == null ? BigDecimal.ZERO : card.getLockedAmount();
        if (wallet.getFrozenBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Frozen balance lower than locked card balance");
        }
        wallet.setFrozenBalance(wallet.getFrozenBalance().subtract(amount));
        wallet.setAllocatedBalance(wallet.getAllocatedBalance().add(amount));
        card.setRemainingAmount(card.getRemainingAmount().add(amount));
        card.setLockedAmount(BigDecimal.ZERO);
        card.setStatus(CardStatus.ACTIVE);
        card.setLockedBy(null);
        return UnblockResult.UNBLOCKED;
    }
}
