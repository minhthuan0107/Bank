package com.example.bank.enums.wallet;

public enum WithdrawOrderStatus {
    PENDING_OTP,
    PENDING_ADMIN,
    PROCESSING,
    SUCCESS,
    FAILED,
    CANCELLED,
    EXPIRED
}