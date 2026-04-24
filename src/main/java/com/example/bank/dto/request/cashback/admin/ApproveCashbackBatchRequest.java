package com.example.bank.dto.request.cashback.admin;

import com.example.bank.common.constants.MessageKeys;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ApproveCashbackBatchRequest(

            /**
             * Danh sách user cần duyệt cashback
             */
            @JsonProperty("user_ids")
            @NotEmpty(message = "{" + MessageKeys.VALIDATION_USER_IDS_REQUIRED + "}")
            List<@NotNull(message = "{" + MessageKeys.VALIDATION_USER_ID_REQUIRED + "}") Long> userIds

    ) {}

