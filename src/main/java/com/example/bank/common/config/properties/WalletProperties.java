package com.example.bank.common.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wallet")
public class WalletProperties {
    private int limit;
    private int defaultPageSize;
}
