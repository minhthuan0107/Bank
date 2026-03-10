package com.example.bank.common.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@RequiredArgsConstructor
@Component
public class LocalizationUtils {
    private final MessageSource messageSource;

    public String getLocalizedMessage(String key, Object... args) {
        Locale locale = LocaleContextHolder.getLocale(); // Spring tự xác định từ header hoặc resolver
        return messageSource.getMessage(key, args, locale);
    }

}
