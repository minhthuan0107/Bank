package com.example.bank.dto.response.cashback.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashbackRefundPageResponse {

    @JsonProperty("items")
    private List<CashbackRefundResponse> items;

    @JsonProperty("page")
    private int page;

    @JsonProperty("size")
    private int size;

    @JsonProperty("total_size")
    private long totalSize;

    @JsonProperty("has_next")
    private boolean hasNext;
}