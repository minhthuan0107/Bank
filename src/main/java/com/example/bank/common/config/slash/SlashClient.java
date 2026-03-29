package com.example.bank.common.config.slash;

import com.example.bank.common.config.properties.SlashProperties;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.wallet.s3.SlashCreateCardRequest;
import com.example.bank.dto.request.wallet.s3.SlashUpdateLimitRequest;
import com.example.bank.dto.response.slash.SlashCardDetailResponse;
import com.example.bank.dto.response.wallet.s3.SlashCreateCardResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlashClient {

    private final WebClient webClient;
    private final SlashProperties props;

    // ===== CREATE CARD =====
    public SlashCreateCardResponse createCard(String name,
                                              String productId,
                                              BigDecimal amount) {

        long cents = amount.multiply(BigDecimal.valueOf(100)).longValue();

        String url = props.getBaseUrl()
                + props.getEndpoints().getCreateCard();

        SlashCreateCardRequest body =
                SlashCreateCardRequest.builder()
                        .type("virtual")
                        .name(name)
                        .accountId(props.getAccount().getId())
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

        try {
            SlashCreateCardResponse response = webClient.post()
                    .uri(url)
                    .headers(h -> h.addAll(buildHeaders()))
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(errBody -> {
                                        log.error("SLASH-CREATE-CARD-ERROR status={} body={}",
                                                clientResponse.statusCode(), errBody);
                                        return Mono.error(new WalletException(
                                                MessageKeys.SLASH_CREATE_CARD_FAILED,
                                                HttpStatus.BAD_GATEWAY));
                                    })
                    )
                    .bodyToMono(SlashCreateCardResponse.class)
                    .block();

            if (response == null) {
                throw new RuntimeException("SLASH_EMPTY_RESPONSE");
            }

            return response;

        } catch (WalletException e) {
            throw e;
        } catch (Exception e) {
            log.error("SLASH-CREATE-CARD-ERROR err={}", e.getMessage());
            throw new WalletException(MessageKeys.SLASH_CREATE_CARD_FAILED, HttpStatus.BAD_GATEWAY);
        }
    }

    // ===== GET CARD =====
    public SlashCardDetailResponse getCard(String cardId) {

        String url = props.getBaseUrl()
                + props.getEndpoints().getGetCard()
                .replace("{id}", cardId);

        try {
            return webClient.get()
                    .uri(url)
                    .headers(h -> h.addAll(buildHeaders()))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(errBody -> {
                                        log.error("SLASH-GET-CARD-ERROR status={} body={}",
                                                clientResponse.statusCode(), errBody);
                                        return Mono.error(new WalletException(
                                                MessageKeys.SLASH_GET_CARD_FAILED,
                                                HttpStatus.BAD_GATEWAY));
                                    })
                    )
                    .bodyToMono(SlashCardDetailResponse.class)
                    .block();

        } catch (WalletException e) {
            throw e;
        } catch (Exception e) {
            log.error("SLASH-GET-CARD-ERROR err={}", e.getMessage());
            throw new WalletException(MessageKeys.SLASH_GET_CARD_FAILED, HttpStatus.BAD_GATEWAY);
        }
    }

    // ===== INCREASE LIMIT =====
    public void increaseLimit(String cardId, BigDecimal amount) {

        String url = props.getBaseUrl()
                + props.getEndpoints().getUpdateLimit()
                .replace("{id}", cardId);

        long cents = amount.multiply(BigDecimal.valueOf(100)).longValue();

        SlashUpdateLimitRequest body =
                SlashUpdateLimitRequest.builder()
                        .spendingConstraint(
                                SlashUpdateLimitRequest.SpendingConstraint.builder()
                                        .spendingRule(
                                                SlashUpdateLimitRequest.SpendingRule.builder()
                                                        .utilizationLimit(
                                                                SlashUpdateLimitRequest.UtilizationLimit.builder()
                                                                        .limitAmount(
                                                                                SlashUpdateLimitRequest.LimitAmount.builder()
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

        try {
            webClient.patch()
                    .uri(url)
                    .headers(h -> h.addAll(buildHeaders()))
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(errBody -> {
                                        log.error("SLASH-INCREASE-LIMIT-ERROR cardId={} status={} body={}",
                                                cardId, clientResponse.statusCode(), errBody);
                                        return Mono.error(new WalletException(
                                                MessageKeys.SLASH_UPDATE_LIMIT_FAILED,
                                                HttpStatus.BAD_GATEWAY));
                                    })
                    )
                    .bodyToMono(Void.class)
                    .block();

        } catch (WalletException e) {
            throw e;
        } catch (Exception e) {
            log.error("SLASH-INCREASE-LIMIT-ERROR cardId={} err={}", cardId, e.getMessage());
            throw new WalletException(MessageKeys.SLASH_UPDATE_LIMIT_FAILED, HttpStatus.BAD_GATEWAY);
        }
    }

    // ===== COMMON HEADERS =====
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", props.getApi().getKey());
        return headers;
    }
}
