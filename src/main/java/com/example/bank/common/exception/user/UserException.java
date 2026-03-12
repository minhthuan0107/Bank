package com.example.bank.common.exception.user;

import org.springframework.http.HttpStatus;

public class UserException extends RuntimeException {
    private final String messageKey;
    private final HttpStatus status;

    public UserException (String messageKey, HttpStatus status) {
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
