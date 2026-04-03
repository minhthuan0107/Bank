package com.example.bank.enums.wallet;

public enum CardTransactionStatus {
    PENDING,   // vừa swipe
    SETTLED,   // trừ tiền thành công
    FAILED     // fail / reversed
}