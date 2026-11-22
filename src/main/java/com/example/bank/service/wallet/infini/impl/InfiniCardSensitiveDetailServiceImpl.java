package com.example.bank.service.wallet.infini.impl;

import com.example.bank.common.config.infini.InfiniCardClient;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.constants.RedisKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.response.wallet.user.CardSensitiveDetailResponse;
import com.example.bank.entity.wallet.infini.InfiniCard;
import com.example.bank.enums.wallet.CardStatus;
import com.example.bank.repository.wallet.infini.InfiniCardRepository;
import com.example.bank.service.wallet.infini.InfiniCardSensitiveDetailService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Reveals card details through POST /v2/cards/reveal.
 * PAN, CVV and expiry are never logged, stored or put in exception messages.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InfiniCardSensitiveDetailServiceImpl implements InfiniCardSensitiveDetailService {

    static final int MAX_REVEALS_PER_HOUR = 10;

    private final InfiniCardClient client;
    private final InfiniCardRepository cards;
    private final StringRedisTemplate redis;

    @Override
    public CardSensitiveDetailResponse getSensitiveDetail(Long userId, Long cardId) {
        client.validateConfiguration();
        InfiniCard card = cards.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new WalletException(MessageKeys.CARD_NOT_FOUND_OR_FORBIDDEN, HttpStatus.NOT_FOUND));
        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new WalletException(MessageKeys.INFINI_REVEAL_CARD_NOT_ACTIVE, HttpStatus.CONFLICT);
        }
        checkRateLimit(userId);

        JsonNode data;
        try {
            data = client.reveal(card.getInfiniCardId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (Exception e) {
            log.warn("Card reveal failed userId={} cardId={} reason={}", userId, cardId, e.getClass().getSimpleName());
            throw unavailable();
        }

        String pan = data.path("card_number").asText("").replaceAll("\\s", "");
        String cvv = data.path("cvv").asText("");
        String mmyy = data.path("expiration_mmyy").asText("").replaceAll("\\D", "");
        if (!pan.matches("\\d{12,19}") || !cvv.matches("\\d{3,4}") || !mmyy.matches("(0[1-9]|1[0-2])\\d{2}")) {
            log.warn("Card reveal returned an unexpected format userId={} cardId={}", userId, cardId);
            throw unavailable();
        }
        // Never hand out another card's details if Infini answered for the wrong card.
        if (card.getLast4() != null && !pan.endsWith(card.getLast4())) {
            log.error("Card reveal last digits mismatch userId={} cardId={}", userId, cardId);
            throw unavailable();
        }

        log.info("Card details revealed userId={} cardId={}", userId, cardId);
        return CardSensitiveDetailResponse.builder()
                .pan(pan)
                .cvv(cvv)
                .expiryMonth(Integer.parseInt(mmyy.substring(0, 2)))
                .expiryYear(2000 + Integer.parseInt(mmyy.substring(2)))
                .createdAt(card.getCreatedAt())
                .build();
    }

    // Fails closed: if the counter cannot be checked, details are not revealed.
    private void checkRateLimit(Long userId) {
        Long count;
        try {
            String key = RedisKeys.cardRevealHourly(userId);
            count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, Duration.ofHours(1));
            }
        } catch (Exception e) {
            log.warn("Card reveal rate limit unavailable userId={} reason={}", userId, e.getClass().getSimpleName());
            throw unavailable();
        }
        if (count == null || count > MAX_REVEALS_PER_HOUR) {
            throw new WalletException(MessageKeys.INFINI_REVEAL_RATE_LIMITED, HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    private WalletException unavailable() {
        return new WalletException(MessageKeys.INFINI_REVEAL_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
