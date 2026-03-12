package com.example.bank.common.exception.wallet;

import org.springframework.http.HttpStatus;

public class WalletException extends RuntimeException {
    private final String messageKey;
    private final HttpStatus status;

    public WalletException (String messageKey, HttpStatus status) {
        super(messageKey);
        this.messageKey = messageKey;
        this.status = status;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
