package com.example.bank.service.wallet.infini;

import com.example.bank.entity.wallet.infini.InfiniCard;

/**
 * Local effects of locking an Infini card, each in its own short transaction.
 * Locking moves the card balance from allocated to frozen (as for Slash cards); unlocking moves it back.
 */
public interface InfiniCardLockTransactionService {

    /**
     * Marks an active card of the user as BLOCKED and moves its balance to frozen.
     * Fails if the card is not active or has an unresolved top-up/withdrawal.
     */
    InfiniCard block(Long userId, Long cardId);

    /** Returns a BLOCKED card to ACTIVE and moves its balance back. Returns false if it was not blocked. */
    boolean unblock(Long cardId);
}
