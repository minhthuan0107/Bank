package com.example.bank.dto.response.cashback.admin;

import com.example.bank.enums.user.AccountStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AdminUserResponse {

    private Long id;

    private String username;

    private String email;

    private AccountStatus status;

    @JsonProperty("card_open_limit")
    private Integer cardOpenLimit;

    @JsonProperty("updated_at")
    private Instant updatedAt;
}