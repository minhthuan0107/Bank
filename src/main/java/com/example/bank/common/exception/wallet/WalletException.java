package com.example.bank.common.exception.wallet;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class WalletException extends RuntimeException {
    private final String messageKey;
    private final HttpStatus status;
    private final Object data; // thêm data

    public WalletException(String messageKey, HttpStatus status) {
        this(messageKey, status, null);
    }

    public WalletException(String messageKey, HttpStatus status, Object data) {
        super(messageKey);
        this.messageKey = messageKey;
        this.status = status;
        this.data = data;
    }
}
