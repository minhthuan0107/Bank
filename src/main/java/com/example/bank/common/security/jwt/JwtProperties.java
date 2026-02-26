package com.example.bank.common.security.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "jwt")
/*
    Cấu hình JWT, được load từ application.yml
 */
public class JwtProperties {
    @NotBlank
    private String issuer;

    @NotBlank              // Base64-encoded secret for HS256
    private String secret;

    @NotBlank
    private String refreshSecret;

    @NotBlank
    private String resetSecret;

    @DurationMin(seconds = 60)   // ≥ 1 phút
    private Duration accessTtl;

    @DurationMin(days = 1)       // ≥ 1 ngày
    private Duration refreshTtl;

    @DurationMin(seconds = 60)       // ≥ 1 phút
    private Duration resetTtl;

    @NotNull
    private Duration clockSkew;

}
