package com.example.bank.dto.request.wallet.user;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.enums.wallet.Stablecoin;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
public class DepositPreviewRequest {
    @NotNull(message = "{" + MessageKeys.VALIDATION_CURRENCY_NOT_BLANK + "}")
    @JsonProperty("currency")
    private Stablecoin currency;

    @NotNull(message = "{" + MessageKeys.VALIDATION_AMOUNT_NOT_NULL + "}")
    @DecimalMin(value = "0.0", message = "{" + MessageKeys.VALIDATION_AMOUNT_MIN + "}")
    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("network")
    private String network;
}
