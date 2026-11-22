package com.example.bank.common.config.infini;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "infini")
@Getter
@Setter
public class InfiniProperties {
    private boolean enabled = false;
    private String baseUrl = "https://openapi-sandbox.infini.money";
    private String keyId;
    private String privateKeyPath;
    private String tokenType = "USDT";
    // No default: set INFINI_APPLICANT_EMAIL (an Infini organization member email).
    private String applicantEmail;
    private Integer productId = 2;
}
