package com.example.bank.dto.response.wallet.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Set;

@Getter
@Builder
public class CardCreationConfigResponse {

    @JsonProperty("min_amount")
    private BigDecimal minAmount;

    @JsonProperty("creation_fee")
    private BigDecimal creationFee;

    @JsonProperty("available_balance")
    private BigDecimal availableBalance;

    @JsonProperty("max_amount")
    private BigDecimal maxAmount;

    @JsonProperty("can_create")
    private boolean canCreate;

    @JsonProperty("product_ids")
    private Set<Integer> productIds;
}
