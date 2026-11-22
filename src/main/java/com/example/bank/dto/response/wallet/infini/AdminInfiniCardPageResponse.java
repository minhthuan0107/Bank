package com.example.bank.dto.response.wallet.infini;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminInfiniCardPageResponse {

    @JsonProperty("items")
    private List<AdminInfiniCardResponse> items;

    @JsonProperty("page")
    private int page;

    @JsonProperty("size")
    private int size;

    @JsonProperty("total_size")
    private long totalSize;

    @JsonProperty("has_next")
    private boolean hasNext;
}
