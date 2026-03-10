package com.example.bank.common.exception.auth;


import com.example.bank.common.exception.base.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class OtpException extends BusinessException {
    private final Integer retryAfter;
    public OtpException(String messageKey) {
        super(messageKey);
        this.retryAfter = null;
    }

    public OtpException(String messageKey, Integer retryAfter) {
        super(messageKey);
        this.retryAfter = retryAfter;
    }

    public Integer getRetryAfter() {
        return retryAfter;
    }
}