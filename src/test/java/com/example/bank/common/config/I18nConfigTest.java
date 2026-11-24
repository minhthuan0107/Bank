package com.example.bank.common.config;

import com.example.bank.common.config.i18n.I18nConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class I18nConfigTest {
    @Test void messagesAreEnglishForEveryBrowserLanguage() {
        var request = new MockHttpServletRequest();
        for (String header : List.of("vi,en-US;q=0.9", "vi", "zh-CN", "en-US")) {
            request.removeHeader("Accept-Language");
            request.addHeader("Accept-Language", header);
            request.setPreferredLocales(Locale.LanguageRange.parse(header).stream()
                    .map(r -> Locale.forLanguageTag(r.getRange())).toList());
            assertEquals("en", new I18nConfig().localeResolver().resolveLocale(request).getLanguage(), header);
        }
    }
}
