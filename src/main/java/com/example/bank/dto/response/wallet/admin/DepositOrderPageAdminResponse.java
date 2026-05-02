package com.example.bank.dto.response.wallet.admin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DepositOrderPageAdminResponse {

    private List<DepositOrderListAdminResponse> items;

    private int page;

    private int size;

    @JsonProperty("total_size")
    private long totalSize;

    @JsonProperty("has_next")
    private boolean hasNext;
}