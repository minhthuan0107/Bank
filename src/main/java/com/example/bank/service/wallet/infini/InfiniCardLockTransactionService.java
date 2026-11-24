package com.example.bank.service.wallet.infini;

import com.example.bank.entity.wallet.infini.InfiniCard;
import com.example.bank.enums.wallet.CardLockedBy;

/**
 * Local effects of locking an Infini card, each in its own short transaction.
 * Locking moves the card balance from allocated to frozen (as for Slash cards); unlocking moves it back.
 */
public interface InfiniCardLockTransactionService {

    /**
     * Marks an active card of the user as BLOCKED and moves its balance to frozen.
     * Fails if the card is not active or has an unresolved top-up/withdrawal.
     * An ADMIN lock also takes over a card already locked by USER or ACCOUNT (no money moves again);
     * newlyBlocked is false for such a takeover.
     */
    BlockResult block(Long userId, Long cardId, CardLockedBy actor);

    /**
     * Returns a BLOCKED card to ACTIVE and moves its balance back.
     * Re-checks under the row lock that the actor may release the current lock.
     */
    UnblockResult unblock(Long cardId, CardLockedBy actor);

    record BlockResult(InfiniCard card, boolean newlyBlocked) {}

    enum UnblockResult { UNBLOCKED, NOT_BLOCKED, NOT_ALLOWED }
}
