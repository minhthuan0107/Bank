package com.example.bank.dto.response.wallet.admin;

import com.example.bank.dto.response.wallet.user.WithdrawOrderListResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class WithdrawOrderPageAdminResponse {

    @JsonProperty("items")
    private List<WithdrawOrderListAdminResponse> items;

    @JsonProperty("page")
    private int page;

    @JsonProperty("size")
    private int size;

    @JsonProperty("total_size")
    private long totalSize;

    @JsonProperty("has_next")
    private boolean hasNext;
}

