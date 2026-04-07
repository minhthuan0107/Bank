package com.example.bank.common.config.slash;

import com.example.bank.common.config.properties.SlashProperties;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.wallet.s3.SlashCreateCardRequest;
import com.example.bank.dto.request.wallet.s3.SlashUpdateLimitRequest;
import com.example.bank.dto.response.slash.SlashCardDetailResponse;
import com.example.bank.dto.response.wallet.s3.SlashCreateCardResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

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
    public void setLimit(String cardId, BigDecimal amount) {

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



    public void lockCard(String cardId) {
        updateCardStatus(cardId, CardStatus.PAUSED);
    }

    public void unblockCard(String cardId) {
        updateCardStatus(cardId, CardStatus.ACTIVE);
    }

// ===== PRIVATE =====

    /**
     * Gọi PATCH /card/{cardId} để cập nhật status thẻ.
     * Slash API không có endpoint /freeze hay /unfreeze riêng.
     * Ref: https://docs.slash.com/api-reference/card-patch
     */
    private void updateCardStatus(String cardId, CardStatus status) {
        String url = buildUrl(props.getEndpoints().getUpdateCard(), cardId);
        Map<String, String> body = Map.of("status", status.getValue());
        webClient.patch()
                .uri(url)
                .headers(h -> h.addAll(buildHeaders()))
                .bodyValue(body)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("[empty body]")
                                .flatMap(responseBody -> {
                                    log.error(
                                            "[SLASH][UPDATE-STATUS] Failed — cardId={} status={} body={}",
                                            cardId, response.statusCode(), responseBody
                                    );
                                    return Mono.error(new WalletException(
                                            MessageKeys.SLASH_UPDATE_CARD_FAILED,
                                            HttpStatus.BAD_GATEWAY
                                    ));
                                })
                )
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info(
                        "[SLASH][UPDATE-STATUS] Success — cardId={} newStatus={}", cardId, status
                ))
                .doOnError(e -> !(e instanceof WalletException), e ->
                        log.error(
                                "[SLASH][UPDATE-STATUS] Unexpected error — cardId={} newStatus={}",
                                cardId, status, e
                        )
                )
                .onErrorMap(e -> !(e instanceof WalletException), e ->
                        new WalletException(MessageKeys.SLASH_UPDATE_CARD_FAILED, HttpStatus.BAD_GATEWAY)
                )
                .block();
    }

    private String buildUrl(String endpointTemplate, String cardId) {
        return UriComponentsBuilder
                .fromHttpUrl(props.getBaseUrl() + endpointTemplate)
                .buildAndExpand(cardId)
                .toUriString();
    }

    // ===== COMMON HEADERS =====
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", props.getApi().getKey());
        return headers;
    }

    // ===== ENUMS =====
    @Getter
    @RequiredArgsConstructor
    private enum CardStatus {
        ACTIVE("active"),
        PAUSED("paused"),
        INACTIVE("inactive"),
        CLOSED("closed");
        private final String value;
    }
}
