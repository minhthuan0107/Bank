package com.example.bank.common.exception.auth;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {

    private final String messageKey;
    private final HttpStatus status;

    public AuthException(String messageKey, HttpStatus status) {
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