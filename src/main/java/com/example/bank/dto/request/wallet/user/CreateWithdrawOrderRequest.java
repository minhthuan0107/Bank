package com.example.bank.dto.request.wallet.user;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.enums.wallet.Stablecoin;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateWithdrawOrderRequest {

    @NotNull(message = "{" + MessageKeys.VALIDATION_WITHDRAW_AMOUNT_NOT_NULL + "}")
    @DecimalMin(
            value = "0.0",
            inclusive = false,
            message = "{" + MessageKeys.VALIDATION_WITHDRAW_AMOUNT_MIN + "}"
    )
    private BigDecimal amount;
}