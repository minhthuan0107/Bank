package com.example.bank.common.config.slash;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.wallet.s3.SlashCreateCardRequest;
import com.example.bank.dto.response.wallet.s3.SlashCreateCardResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;




@Component
@RequiredArgsConstructor
@Slf4j
public class SlashClient {

    private final RestTemplate restTemplate;

    @Value("${slash.api.key}")
    private String apiKey;

    @Value("${slash.account.id}")
    private String accountId;

    private static final String CREATE_CARD_URL = "https://api.slash.com/card";

    public SlashCreateCardResponse createCard(String name,
                                              String productId,
                                              BigDecimal amount) {

        long cents = amount.multiply(BigDecimal.valueOf(100)).longValue();

        SlashCreateCardRequest body =
                SlashCreateCardRequest.builder()
                        .type("virtual")
                        .name(name)
                        .accountId(accountId)
                        .cardProductId(productId)
                        .spendingConstraint(
                                SlashCreateCardRequest.SpendingConstraint.builder()
                                        .spendingRule(
                                                SlashCreateCardRequest.SpendingRule.builder()
                                                        .utilizationLimit(
                                                                SlashCreateCardRequest.UtilizationLimit.builder()
                                                                        .limitAmount(
                                                                                SlashCreateCardRequest.LimitAmount.builder()
                                                                                        .amountCents(cents)
                                                                                        .build()
                                                                        )
                                                                        .preset("collective")
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )
                        .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", apiKey);

        HttpEntity<SlashCreateCardRequest> request =
                new HttpEntity<>(body, headers);

        try {
            ResponseEntity<SlashCreateCardResponse> response =
                    restTemplate.exchange(
                            CREATE_CARD_URL,
                            HttpMethod.POST,
                            request,
                            SlashCreateCardResponse.class
                    );

            if (response.getBody() == null) {
                throw new RuntimeException("SLASH_EMPTY_RESPONSE");
            }

            return response.getBody();

        } catch (HttpStatusCodeException ex) {
            log.error("SLASH-CREATE-CARD-ERROR status={} body={}",
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString()
            );
            throw new WalletException(
                    MessageKeys.SLASH_CREATE_CARD_FAILED,
                    HttpStatus.BAD_GATEWAY
            );
        }
    }
}