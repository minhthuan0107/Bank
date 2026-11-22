package com.example.bank.common.config.infini;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.entity.wallet.infini.CardCreationOrder;
import com.example.bank.entity.wallet.infini.InfiniFundingOrder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
@RequiredArgsConstructor
public class InfiniCardClient {
    private final InfiniProperties properties;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private volatile PrivateKey privateKey;

    public void validateConfiguration() {
        try {
            URI origin = URI.create(properties.getBaseUrl());
            if (!properties.isEnabled() || !"https".equals(origin.getScheme()) ||
                    origin.getHost() == null || origin.getUserInfo() != null ||
                    origin.getQuery() != null || origin.getFragment() != null ||
                    !(origin.getPath().isEmpty() || "/".equals(origin.getPath())) ||
                    properties.getKeyId() == null ||
                    !properties.getKeyId().matches("[1-9A-HJ-NP-Za-km-z]{32}") ||
                    !Set.of("USDT", "USDC").contains(properties.getTokenType()) ||
                    properties.getProductId() == null ||
                    !Set.of(1, 2, 102).contains(properties.getProductId()) ||
                    properties.getApplicantEmail() == null ||
                    !properties.getApplicantEmail().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
                throw new IllegalStateException("Invalid Infini configuration");
            }
            loadKey();
        } catch (Exception e) {
            throw new WalletException(MessageKeys.CARD_CREATION_PROVIDER_NOT_CONFIGURED, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private synchronized PrivateKey loadKey() throws Exception {
        if (privateKey == null) {
            String pem = Files.readString(Path.of(properties.getPrivateKeyPath()), StandardCharsets.US_ASCII).trim();
            if (!pem.startsWith("-----BEGIN PRIVATE KEY-----") || !pem.endsWith("-----END PRIVATE KEY-----")) {
                throw new IllegalArgumentException("Expected PKCS8 private key");
            }
            String encoded = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
            privateKey = KeyFactory.getInstance("Ed25519").generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encoded)));
        }
        return privateKey;
    }

    public JsonNode apply(CardCreationOrder order) throws Exception {
        byte[] body = mapper.writeValueAsBytes(Map.of(
                "product_id", order.getProductId(),
                "top_up_amount", order.getAmount().toPlainString(),
                "token_type", order.getTokenType(),
                "user_email", order.getApplicantEmail(),
                "holder_name", order.getHolderName(),
                // The request ID is the alias, so an unresolved application can be found again via /list.
                "card_alias", order.getRequestId()));
        // No retry: the documented apply request has no provider idempotency key.
        try {
            return send("POST", "/v2/cards/apply", body, APPLY_TIMEOUT);
        } catch (BusinessError e) {
            // Documented: HTTP 200 with a non-zero code is a business error, not a created card.
            throw new DefiniteRejection("PROVIDER_ERROR_" + e.getCode());
        }
    }

    /** POST /v2/cards/top-up: moves funds from the business account to the card. */
    public JsonNode topUp(InfiniFundingOrder order) throws Exception {
        return fund("/v2/cards/top-up", order);
    }

    /** POST /v2/cards/redeem: moves funds from the card back to the business account. */
    public JsonNode redeem(InfiniFundingOrder order) throws Exception {
        return fund("/v2/cards/redeem", order);
    }

    private JsonNode fund(String target, InfiniFundingOrder order) throws Exception {
        byte[] body = mapper.writeValueAsBytes(Map.of(
                "id", order.getProviderCardId(),
                "amount", order.getAmount().setScale(2, java.math.RoundingMode.UNNECESSARY).toPlainString(),
                "token_type", order.getTokenType(),
                // Lets an operator match the provider record to this order.
                "note", order.getRequestId()));
        // No retry: the documented funding requests have no provider idempotency key.
        try {
            return send("POST", target, body, APPLY_TIMEOUT);
        } catch (BusinessError e) {
            throw new DefiniteRejection("PROVIDER_ERROR_" + e.getCode());
        }
    }

    /** Cards whose alias equals the given value (GET /v2/cards/list). */
    public List<JsonNode> findByAlias(String alias) throws Exception {
        List<JsonNode> matches = new ArrayList<>();
        for (int page = 1; page <= MAX_LIST_PAGES; page++) {
            JsonNode data = send("GET", "/v2/cards/list?card_alias=" +
                    URLEncoder.encode(alias, StandardCharsets.UTF_8) + "&page=" + page + "&page_size=100",
                    new byte[0], STATUS_TIMEOUT);
            JsonNode cards = data.path("cards");
            if (!cards.isArray()) {
                throw new IllegalStateException("Unexpected Infini list response");
            }
            for (JsonNode card : cards) {
                // The filter semantics are not documented, so compare the alias exactly.
                if (alias.equals(card.path("card_alias").asText())) matches.add(card);
            }
            if (page >= data.path("total_pages").asInt(1)) break;
        }
        return matches;
    }

    public JsonNode status(String id) throws Exception {
        return send("GET", "/v2/cards/status?id=" +
                URLEncoder.encode(id, StandardCharsets.UTF_8), new byte[0], STATUS_TIMEOUT);
    }

    // Rejected before business execution, so no card was created.
    private static final Set<Integer> DEFINITE_REJECTION_STATUSES = Set.of(400, 401, 403, 404, 422, 429);
    private static final Duration APPLY_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration STATUS_TIMEOUT = Duration.ofSeconds(8);
    private static final int MAX_LIST_PAGES = 5;

    private JsonNode send(String method, String target, byte[] body, Duration timeout) throws Exception {
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        byte[] random = new byte[16];
        new SecureRandom().nextBytes(random);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        String digest = InfiniSigner.digest(body);
        String signature = InfiniSigner.sign(loadKey(), properties.getKeyId(), method,
                target, timestamp, nonce, digest);
        String origin = properties.getBaseUrl().replaceAll("/$", "");
        HttpRequest request = HttpRequest.newBuilder(URI.create(origin + target))
                .timeout(timeout)
                .header("X-API-Key-ID", properties.getKeyId())
                .header("X-API-Timestamp", timestamp)
                .header("X-API-Nonce", nonce)
                .header("Digest", digest)
                .header("Authorization", "Ed25519 signature=\"" + signature + "\"")
                .header("Content-Type", "application/json")
                .method(method, body.length == 0 ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofByteArray(body)).build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        // The gateway / validation layer rejects these before business execution.
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new DefiniteRejection("AUTHENTICATION_REJECTED");
        }
        if (DEFINITE_REJECTION_STATUSES.contains(response.statusCode())) {
            throw new DefiniteRejection("PROVIDER_REJECTED");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Infini result requires reconciliation");
        }
        JsonNode root = mapper.readTree(response.body());
        if (root != null && root.path("code").isIntegralNumber() && root.path("code").intValue() != 0) {
            // Only the numeric code is kept; the message is free text and is never stored or logged.
            throw new BusinessError(root.path("code").intValue());
        }
        if (root == null || !root.path("code").isIntegralNumber() || !root.path("data").isObject()) {
            throw new IllegalStateException("Infini business result requires reconciliation");
        }
        return root.get("data");
    }

    /** HTTP 2xx with a non-zero business code. */
    public static class BusinessError extends Exception {
        private final int code;

        public BusinessError(int code) {
            super("Infini business error " + code, null, false, false);
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    public static class DefiniteRejection extends Exception {
        private final String reason;

        public DefiniteRejection(String reason) {
            super(reason, null, false, false);
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }
    }
}
