package com.example.bank.common.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "slash")
@Data
public class SlashProperties {

    private String baseUrl;

    private Api api;
    private Account account;
    private Endpoints endpoints;

    @Data
    public static class Api {
        private String key;
    }

    @Data
    public static class Account {
        private String id;
    }

    @Data
    public static class Endpoints {
        private String createCard;
        private String getCard;
        private String updateLimit;
        private String updateCard;
    }
}