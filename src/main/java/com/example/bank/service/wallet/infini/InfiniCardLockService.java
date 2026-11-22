package com.example.bank.service.wallet.infini;

/** Lock (freeze) and unlock (unfreeze) Infini cards. */
public interface InfiniCardLockService {

    void lockCard(Long userId, Long cardId);

    void unlockCard(Long userId, Long cardId);

    /** Locks every active Infini card of a user (admin locked the account). Failures are logged per card. */
    void lockAllCardsOfUser(Long userId);

    /** Unlocks every blocked Infini card of a user (admin unlocked the account). Failures are logged per card. */
    void unlockAllCardsOfUser(Long userId);
}
