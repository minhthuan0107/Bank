package com.example.bank.enums.wallet;

public enum CardTransactionStatus {
    PENDING,   // vừa swipe
    POSTED,   // trừ tiền thành công
    FAILED,
    REVERSED,
    UNKNOWN,

}