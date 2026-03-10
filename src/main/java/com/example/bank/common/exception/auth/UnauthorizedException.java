package com.example.bank.common.exception.auth;

import com.example.bank.common.exception.base.HasMessageKey;
import org.springframework.http.HttpStatus;


public class UnauthorizedException extends RuntimeException implements HasMessageKey {
    private final String messageKey;
    private final HttpStatus status;

    public UnauthorizedException(String messageKey,HttpStatus status) {
        super(messageKey);
        this.messageKey = messageKey;
        this.status = status;
    }
    @Override
    public String getMessageKey() {
        return messageKey;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
