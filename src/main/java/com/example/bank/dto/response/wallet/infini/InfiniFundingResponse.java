package com.example.bank.dto.response.wallet.infini;

import com.example.bank.entity.wallet.infini.InfiniFundingOrder;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class InfiniFundingResponse {

    @JsonProperty("request_id")
    private String requestId;

    // TOP_UP or REDEEM
    @JsonProperty("type")
    private String type;

    // PENDING, REVIEW_REQUIRED, SUCCEEDED or FAILED
    @JsonProperty("status")
    private String status;

    // Local ID in cards_infini.
    @JsonProperty("card_id")
    private Long cardId;

    @JsonProperty("amount")
    private BigDecimal amount;

    // Card balance reported by Infini after the operation, when known.
    @JsonProperty("card_balance")
    private BigDecimal cardBalance;

    // Known reason of a FAILED order (TOP_UP_TOO_SMALL, WITHDRAW_COOLDOWN), otherwise null.
    @JsonProperty("failure_reason")
    private String failureReason;

    private static String failureReason(InfiniFundingOrder order) {
        if (!InfiniFundingOrder.FAILED.equals(order.getStatus()) || order.getReviewReason() == null) return null;
        return switch (order.getReviewReason()) {
            case "PROVIDER_ERROR_21021" -> "TOP_UP_TOO_SMALL"; // Seen for 1 USD; not documented by Infini.
            case "PROVIDER_ERROR_21026" -> "WITHDRAW_COOLDOWN";
            default -> null;
        };
    }

    public static InfiniFundingResponse from(InfiniFundingOrder order) {
        return InfiniFundingResponse.builder()
                .requestId(order.getRequestId())
                .type(order.getType())
                .status(order.getStatus())
                .cardId(order.getCardId())
                .amount(order.getAmount())
                .cardBalance(order.getProviderCardBalance())
                .failureReason(failureReason(order))
                .build();
    }
}
