package com.example.bank.common.config.slash;

import com.example.bank.common.config.properties.SlashProperties;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.wallet.s3.SlashCreateCardRequest;
import com.example.bank.dto.request.wallet.s3.SlashUpdateLimitRequest;
import com.example.bank.dto.response.slash.SlashCardDetailResponse;
import com.example.bank.dto.response.wallet.s3.SlashCreateCardResponse;
import com.example.bank.dto.response.wallet.user.SlashCardSensitiveDetailResponse;
import com.example.bank.dto.response.wallet.user.SlashTransactionResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlashClient {

    private static final int GET_TRANSACTION_MAX_ATTEMPTS = 3;

    private static final Duration GET_TRANSACTION_TIMEOUT =
            Duration.ofSeconds(2);

    private static final Duration GET_TRANSACTION_RETRY_DELAY =
            Duration.ofMillis(250);

    private final WebClient webClient;
    private final SlashProperties props;

    // ===== CREATE CARD =====

    public SlashCreateCardResponse createCard(
            String name,
            String productId,
            BigDecimal amount
    ) {
        long cents = amount
                .multiply(BigDecimal.valueOf(100))
                .longValue();

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
                    .onStatus(
                            HttpStatusCode::isError,
                            clientResponse ->
                                    clientResponse.bodyToMono(String.class)
                                            .flatMap(errBody -> {
                                                log.error(
                                                        "SLASH-CREATE-CARD-ERROR status={} body={}",
                                                        clientResponse.statusCode(),
                                                        errBody
                                                );

                                                return Mono.error(
                                                        new WalletException(
                                                                MessageKeys.SLASH_CREATE_CARD_FAILED,
                                                                HttpStatus.BAD_GATEWAY
                                                        )
                                                );
                                            })
                    )
                    .bodyToMono(SlashCreateCardResponse.class)
                    .block();

            if (response == null) {
                throw new WalletException(
                        MessageKeys.SLASH_CREATE_CARD_FAILED,
                        HttpStatus.BAD_GATEWAY
                );
            }

            return response;

        } catch (WalletException e) {
            throw e;

        } catch (Exception e) {
            log.error(
                    "SLASH-CREATE-CARD-ERROR err={}",
                    e.getMessage(),
                    e
            );

            throw new WalletException(
                    MessageKeys.SLASH_CREATE_CARD_FAILED,
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    // ===== GET CARD =====

    public SlashCardDetailResponse getCard(String cardId) {
        String url = props.getBaseUrl()
                + props.getEndpoints().getGetCard()
                .replace("{id}", cardId);

        try {
            SlashCardDetailResponse response = webClient.get()
                    .uri(url)
                    .headers(h -> h.addAll(buildHeaders()))
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            clientResponse ->
                                    clientResponse.bodyToMono(String.class)
                                            .flatMap(errBody -> {
                                                log.error(
                                                        "SLASH-GET-CARD-ERROR cardId={} status={} body={}",
                                                        cardId,
                                                        clientResponse.statusCode(),
                                                        errBody
                                                );

                                                return Mono.error(
                                                        new WalletException(
                                                                MessageKeys.SLASH_GET_CARD_FAILED,
                                                                HttpStatus.BAD_GATEWAY
                                                        )
                                                );
                                            })
                    )
                    .bodyToMono(SlashCardDetailResponse.class)
                    .block();

            if (response == null) {
                throw new WalletException(
                        MessageKeys.SLASH_GET_CARD_FAILED,
                        HttpStatus.BAD_GATEWAY
                );
            }

            return response;

        } catch (WalletException e) {
            throw e;

        } catch (Exception e) {
            log.error(
                    "SLASH-GET-CARD-ERROR cardId={} err={}",
                    cardId,
                    e.getMessage(),
                    e
            );

            throw new WalletException(
                    MessageKeys.SLASH_GET_CARD_FAILED,
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    // ===== UPDATE LIMIT =====

    public void setLimit(
            String cardId,
            BigDecimal amount
    ) {
        String url = props.getBaseUrl()
                + props.getEndpoints().getUpdateLimit()
                .replace("{id}", cardId);

        long cents = amount
                .multiply(BigDecimal.valueOf(100))
                .longValue();

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
                    .onStatus(
                            HttpStatusCode::isError,
                            clientResponse ->
                                    clientResponse.bodyToMono(String.class)
                                            .flatMap(errBody -> {
                                                log.error(
                                                        "SLASH-INCREASE-LIMIT-ERROR cardId={} status={} body={}",
                                                        cardId,
                                                        clientResponse.statusCode(),
                                                        errBody
                                                );

                                                return Mono.error(
                                                        new WalletException(
                                                                MessageKeys.SLASH_UPDATE_LIMIT_FAILED,
                                                                HttpStatus.BAD_GATEWAY
                                                        )
                                                );
                                            })
                    )
                    .bodyToMono(Void.class)
                    .block();

        } catch (WalletException e) {
            throw e;

        } catch (Exception e) {
            log.error(
                    "SLASH-INCREASE-LIMIT-ERROR cardId={} err={}",
                    cardId,
                    e.getMessage(),
                    e
            );

            throw new WalletException(
                    MessageKeys.SLASH_UPDATE_LIMIT_FAILED,
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    // ===== CARD STATUS =====

    public void lockCard(String cardId) {
        updateCardStatus(
                cardId,
                CardStatus.PAUSED
        );
    }

    public void unblockCard(String cardId) {
        updateCardStatus(
                cardId,
                CardStatus.ACTIVE
        );
    }

    private void updateCardStatus(
            String cardId,
            CardStatus status
    ) {
        String url = buildUrl(
                props.getEndpoints().getUpdateCard(),
                cardId
        );

        Map<String, String> body =
                Map.of("status", status.getValue());

        webClient.patch()
                .uri(url)
                .headers(h -> h.addAll(buildHeaders()))
                .bodyValue(body)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        response ->
                                response.bodyToMono(String.class)
                                        .defaultIfEmpty("[empty body]")
                                        .flatMap(responseBody -> {
                                            log.error(
                                                    "[SLASH][UPDATE-STATUS] Failed — cardId={} status={} body={}",
                                                    cardId,
                                                    response.statusCode(),
                                                    responseBody
                                            );

                                            return Mono.error(
                                                    new WalletException(
                                                            MessageKeys.SLASH_UPDATE_CARD_FAILED,
                                                            HttpStatus.BAD_GATEWAY
                                                    )
                                            );
                                        })
                )
                .bodyToMono(Void.class)
                .doOnSuccess(v ->
                        log.info(
                                "[SLASH][UPDATE-STATUS] Success — cardId={} newStatus={}",
                                cardId,
                                status
                        )
                )
                .doOnError(
                        e -> !(e instanceof WalletException),
                        e -> log.error(
                                "[SLASH][UPDATE-STATUS] Unexpected error — cardId={} newStatus={}",
                                cardId,
                                status,
                                e
                        )
                )
                .onErrorMap(
                        e -> !(e instanceof WalletException),
                        e -> new WalletException(
                                MessageKeys.SLASH_UPDATE_CARD_FAILED,
                                HttpStatus.BAD_GATEWAY
                        )
                )
                .block();
    }

    public SlashTransactionResponse getTransaction(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException(
                    "transactionId is blank"
            );
        }

        String url = props.getBaseUrl()
                + props.getEndpoints().getGetTransaction()
                .replace("{id}", transactionId);

        log.debug(
                "SLASH_GET_TRANSACTION_REQUEST txId={}",
                transactionId
        );

        try {
            SlashTransactionResponse response = webClient.get()
                    .uri(url)
                    .headers(headers -> headers.addAll(buildHeaders()))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(SlashTransactionResponse.class)
                    .timeout(GET_TRANSACTION_TIMEOUT)
                    .retryWhen(
                            Retry.backoff(
                                            GET_TRANSACTION_MAX_ATTEMPTS - 1,
                                            GET_TRANSACTION_RETRY_DELAY
                                    )
                                    .maxBackoff(Duration.ofSeconds(1))
                                    .filter(this::isRetryableTransactionError)
                                    .doBeforeRetry(signal ->
                                            log.warn(
                                                    "SLASH_GET_TRANSACTION_RETRY txId={} nextAttempt={}/{} reason={}",
                                                    transactionId,
                                                    signal.totalRetries() + 2,
                                                    GET_TRANSACTION_MAX_ATTEMPTS,
                                                    signal.failure().getMessage()
                                            )
                                    )
                                    .onRetryExhaustedThrow(
                                            (spec, signal) ->
                                                    signal.failure()
                                    )
                    )
                    .block();

            if (response == null
                    || response.getId() == null
                    || response.getId().isBlank()) {

                log.error(
                        "SLASH_GET_TRANSACTION_EMPTY_RESPONSE txId={}",
                        transactionId
                );

                throw new WalletException(
                        MessageKeys.SLASH_GET_TRANSACTION_FAILED,
                        HttpStatus.BAD_GATEWAY
                );
            }

            log.debug(
                    "SLASH_GET_TRANSACTION_SUCCESS txId={} status={} detailedStatus={}",
                    response.getId(),
                    response.getStatus(),
                    response.getDetailedStatus()
            );

            return response;

        } catch (WalletException e) {
            throw e;

        } catch (WebClientResponseException e) {
            log.error(
                    "SLASH_GET_TRANSACTION_FAILED txId={} status={} body={}",
                    transactionId,
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString()
            );

            throw new WalletException(
                    MessageKeys.SLASH_GET_TRANSACTION_FAILED,
                    HttpStatus.BAD_GATEWAY
            );

        } catch (Exception e) {
            log.error(
                    "SLASH_GET_TRANSACTION_FAILED txId={} message={}",
                    transactionId,
                    e.getMessage(),
                    e
            );

            throw new WalletException(
                    MessageKeys.SLASH_GET_TRANSACTION_FAILED,
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    private boolean isRetryableTransactionError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException e) {
            int statusCode = e.getStatusCode().value();

            return statusCode == 408
                    || statusCode == 429
                    || statusCode >= 500;
        }

        return throwable instanceof WebClientRequestException
                || throwable instanceof TimeoutException;
    }

    // ===== GET CARD SENSITIVE =====

    public SlashCardSensitiveDetailResponse getCardSensitiveDetail(
            String cardId
    ) {
        if (cardId == null || cardId.isBlank()) {
            throw new WalletException(
                    MessageKeys.SLASH_CARD_ID_INVALID,
                    HttpStatus.BAD_REQUEST
            );
        }

        String url = UriComponentsBuilder
                .fromHttpUrl(
                        props.getVaultBaseUrl()
                                + props.getEndpoints().getGetCard()
                )
                .queryParam("include_pan", "true")
                .queryParam("include_cvv", "true")
                .buildAndExpand(cardId)
                .toUriString();

        try {
            SlashCardSensitiveDetailResponse response = webClient.get()
                    .uri(url)
                    .header(
                            "X-API-Key",
                            props.getApi().getKey()
                    )
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            clientResponse ->
                                    clientResponse.bodyToMono(String.class)
                                            .defaultIfEmpty("[empty body]")
                                            .flatMap(errBody -> {
                                                log.error(
                                                        "SLASH-GET-CARD-SENSITIVE-ERROR cardId={} status={} body={}",
                                                        cardId,
                                                        clientResponse.statusCode(),
                                                        errBody
                                                );

                                                return Mono.error(
                                                        new WalletException(
                                                                MessageKeys.SLASH_GET_CARD_SENSITIVE_FAILED,
                                                                HttpStatus.BAD_GATEWAY
                                                        )
                                                );
                                            })
                    )
                    .bodyToMono(
                            SlashCardSensitiveDetailResponse.class
                    )
                    .timeout(Duration.ofSeconds(5))
                    .block();

            if (response == null) {
                throw new WalletException(
                        MessageKeys.SLASH_GET_CARD_SENSITIVE_FAILED,
                        HttpStatus.BAD_GATEWAY
                );
            }

            return response;

        } catch (WalletException e) {
            throw e;

        } catch (Exception e) {
            log.error(
                    "SLASH-GET-CARD-SENSITIVE-ERROR cardId={} err={}",
                    cardId,
                    e.getMessage(),
                    e
            );

            throw new WalletException(
                    MessageKeys.SLASH_GET_CARD_SENSITIVE_FAILED,
                    HttpStatus.BAD_GATEWAY
            );
        }
    }



    // ===== COMMON =====
    private String buildUrl(
            String endpointTemplate,
            String id
    ) {
        return UriComponentsBuilder
                .fromHttpUrl(
                        props.getBaseUrl()
                                + endpointTemplate
                )
                .buildAndExpand(id)
                .toUriString();
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        headers.set(
                "X-API-Key",
                props.getApi().getKey()
        );

        return headers;
    }

    // ===== ENUM =====

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