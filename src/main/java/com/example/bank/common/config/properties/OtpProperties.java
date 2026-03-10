package com.example.bank.common.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
@Data
@Component
@ConfigurationProperties(prefix = "otp")
public class OtpProperties {

    private int ttlSeconds;
    private int cooldownSeconds;

    private Limit limit = new Limit();

    @Data
    public static class Limit {

        private int perHour;
        private int ipHourly;

    }
}
