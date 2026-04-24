package com.example.bank.dto.response.cashback.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class CashbackHistoryPageResponse {
    private List<CashbackHistoryResponse> items;

    private int page;

    private int size;

    @JsonProperty("total_size")
    private long totalSize;

    @JsonProperty("has_next")
    private boolean hasNext;

    @JsonProperty("updated_at")
    private final Instant updateAt;
}
