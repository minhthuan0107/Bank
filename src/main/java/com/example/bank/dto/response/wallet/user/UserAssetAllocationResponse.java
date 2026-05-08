package com.example.bank.dto.response.wallet.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;


@Builder
public record UserAssetAllocationResponse(

        @JsonProperty("total_amount")
        BigDecimal totalAmount,

        @JsonProperty("items")
        List<UserAssetAllocationItemResponse> items
) {
}
