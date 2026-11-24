package com.example.bank.enums.wallet;

/** Who locked a card, and therefore who may unlock it. */
public enum CardLockedBy {
    /** The card owner. */
    USER,
    /** An admin locked the whole account. */
    ACCOUNT,
    /** An admin locked this card. */
    ADMIN;

    /** ADMIN may release any lock; the others only their own. */
    public boolean mayRelease(CardLockedBy lock) {
        return this == ADMIN || this == lock;
    }
}
