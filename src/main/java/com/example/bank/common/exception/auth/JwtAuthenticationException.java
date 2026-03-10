package com.example.bank.common.exception.auth;

import com.example.bank.common.exception.base.HasMessageKey;
import lombok.Getter;

@Getter
public class JwtAuthenticationException extends RuntimeException implements HasMessageKey {
    private final String messageKey;

    public JwtAuthenticationException(String messageKey) {

        this.messageKey = messageKey;
    }
    @Override
    public String getMessageKey() {
        return messageKey;
    }
}