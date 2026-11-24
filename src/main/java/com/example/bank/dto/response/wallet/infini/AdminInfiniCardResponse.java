package com.example.bank.dto.response.wallet.infini;

import com.example.bank.entity.wallet.infini.InfiniCard;
import com.example.bank.enums.wallet.CardLockedBy;
import com.example.bank.enums.wallet.CardStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/** An Infini card as shown to admins. Never contains PAN, CVV or the Infini card ID. */
@Getter
@Builder
public class AdminInfiniCardResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("username")
    private String username;

    @Builder.Default
    @JsonProperty("provider")
    private String provider = "INFINI";

    @JsonProperty("product_id")
    private Integer productId;

    @JsonProperty("masked_card")
    private String maskedCard;

    @JsonProperty("name")
    private String name;

    @JsonProperty("holder_name")
    private String holderName;

    @JsonProperty("note")
    private String note;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("status")
    private CardStatus status;

    // USER, ACCOUNT or ADMIN while locked; only USER locks can be released by the owner.
    @JsonProperty("locked_by")
    private CardLockedBy lockedBy;

    @JsonProperty("remaining_amount")
    private BigDecimal remainingAmount;

    // Balance moved to frozen while the card is locked.
    @JsonProperty("locked_amount")
    private BigDecimal lockedAmount;

    @JsonProperty("created_at")
    private Instant createdAt;

    public static AdminInfiniCardResponse from(InfiniCard card, String username) {
        return AdminInfiniCardResponse.builder()
                .id(card.getId())
                .userId(card.getUserId())
                .username(username)
                .productId(card.getProductId())
                .maskedCard(card.getLast4() == null ? "****" : "**** **** **** " + card.getLast4())
                .name(card.getName())
                .holderName(card.getHolderName())
                .note(card.getNote())
                .currency(card.getCurrency())
                .status(card.getStatus())
                .lockedBy(card.getLockedBy())
                .remainingAmount(card.getRemainingAmount())
                .lockedAmount(card.getLockedAmount())
                .createdAt(card.getCreatedAt())
                .build();
    }
}
