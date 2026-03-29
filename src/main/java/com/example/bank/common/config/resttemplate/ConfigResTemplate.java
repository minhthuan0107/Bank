package com.example.bank.common.config.resttemplate;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ConfigResTemplate {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
