package com.example.bank.dto.response.cashback.admin;

import com.example.bank.enums.wallet.CashbackStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashbackRefundResponse {

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("total_spent")
    private BigDecimal totalSpent;

    @JsonProperty("cashback_amount")
    private BigDecimal cashbackAmount;

    @JsonProperty("percent")
    private BigDecimal percent; // FE hiển thị level %

    @JsonProperty("month")
    private String month;

    @JsonProperty("status")
    private CashbackStatus cashbackStatus;

    @JsonProperty("updated_at")
    private Instant updatedAt;
}
