package com.example.bank.dto.response.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AnnouncementPageResponse {

    private List<AnnouncementResponse> items;

    private int page;

    private int size;

    @JsonProperty("total_size")
    private long totalSize;

    @JsonProperty("has_next")
    private boolean hasNext;
}
