package com.example.bank.dto.response.wallet.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record CardFundingTransactionPageResponse(

        @JsonProperty("items")
        List<CardFundingTransactionItemResponse> items,

        @JsonProperty("page")
        int page,

        @JsonProperty("size")
        int size,

        @JsonProperty("total_size")
        long totalSize,

        @JsonProperty("has_next")
        boolean hasNext
) {
}
