package com.example.bank.dto.request.cashback.admin;

import com.example.bank.common.constants.MessageKeys;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCardOpenLimitRequest {

    /**
     * Giới hạn số lượng thẻ user được mở
     */
    @JsonProperty("card_open_limit")
    @NotNull(message = "{" + MessageKeys.VALIDATION_CARD_OPEN_LIMIT_REQUIRED + "}")
    @Min(value = 0, message = "{" + MessageKeys.VALIDATION_CARD_OPEN_LIMIT_INVALID + "}")
    @Max(value = 10000, message = "{" + MessageKeys.VALIDATION_CARD_OPEN_LIMIT_INVALID + "}")
    private Integer cardOpenLimit;
}