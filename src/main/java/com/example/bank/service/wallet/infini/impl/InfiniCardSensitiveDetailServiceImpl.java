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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        String pan = data.path("card_number").asText("").replaceAll("[\\s-]", "");
        String cvv = padded(data.path("cvv"), 3);
        YearMonth expiry = parseExpiry(padded(data.path("expiration_mmyy"), 4));
        if (!pan.matches("\\d{12,19}") || !cvv.matches("\\d{3,4}") || expiry == null) {
            // Only field names and masked shapes (9 = digit, A = letter): never the values.
            log.warn("Card reveal returned an unexpected format userId={} cardId={} number={} cvv={} expiry={} fields={}",
                    userId, cardId, shape(data.path("card_number")), shape(data.path("cvv")),
                    shape(data.path("expiration_mmyy")), fieldNames(data));
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
                .expiryMonth(expiry.getMonthValue())
                .expiryYear(expiry.getYear())
                .createdAt(card.getCreatedAt())
                .build();
    }

    // A JSON number loses its leading zeros (e.g. 0829 -> 829).
    private static String padded(JsonNode node, int length) {
        String text = node.asText("").trim();
        return node.isIntegralNumber() && text.length() < length
                ? "0".repeat(length - text.length()) + text : text;
    }

    /** Accepts MMYY and MMYYYY, with or without a separator, and YYYY-MM / YYYY/MM. Null if unreadable. */
    static YearMonth parseExpiry(String value) {
        Matcher yearFirst = Pattern.compile("(20\\d{2})[/\\-. ](\\d{1,2})").matcher(value);
        Matcher monthFirst = Pattern.compile("(\\d{1,2})\\s*[/\\-. ]\\s*((?:20)?\\d{2})|(\\d{2})((?:20)?\\d{2})").matcher(value);
        String month;
        String year;
        if (yearFirst.matches()) {
            year = yearFirst.group(1);
            month = yearFirst.group(2);
        } else if (monthFirst.matches()) {
            month = monthFirst.group(1) != null ? monthFirst.group(1) : monthFirst.group(3);
            year = monthFirst.group(2) != null ? monthFirst.group(2) : monthFirst.group(4);
        } else {
            return null;
        }
        int m = Integer.parseInt(month);
        int y = Integer.parseInt(year);
        return m < 1 || m > 12 ? null : YearMonth.of(y < 100 ? 2000 + y : y, m);
    }

    private static String shape(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) return "missing";
        if (node.isContainerNode()) return node.getNodeType().name();
        String masked = node.asText().replaceAll("\\d", "9").replaceAll("\\p{L}", "A");
        return node.getNodeType() + "(" + (masked.length() > 40 ? "length " + masked.length() : masked) + ")";
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
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
