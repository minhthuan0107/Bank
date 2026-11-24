package com.example.bank.service.wallet.infini;

import com.example.bank.enums.wallet.CardLockedBy;

/** Lock (freeze) and unlock (unfreeze) Infini cards. */
public interface InfiniCardLockService {

    /** userId is the card owner; actor is who performs the lock. */
    void lockCard(Long userId, Long cardId, CardLockedBy actor);

    /** Fails with 403 if the actor may not release the current lock (e.g. the owner on an admin lock). */
    void unlockCard(Long userId, Long cardId, CardLockedBy actor);

    /** Locks every active Infini card of a user (admin locked the account). Failures are logged per card. */
    void lockAllCardsOfUser(Long userId);

    /** Unlocks the cards locked by lockAllCardsOfUser; cards locked by the owner or by an admin stay locked. */
    void unlockAllCardsOfUser(Long userId);
}
