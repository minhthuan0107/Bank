package com.example.bank.common.exception.auth;

import com.example.bank.common.exception.base.HasMessageKey;
/*
 * Exception được throw khi user đăng nhập sai cổng
 */
public class ForbiddenLoginException extends RuntimeException implements HasMessageKey {
    private final String messageKey;

    // Constructor
    public ForbiddenLoginException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
    }


    // Getter
    @Override
    public String getMessageKey() {
        return messageKey;
    }
}
