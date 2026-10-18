package com.example.bank.dto.request.wallet.user;

import com.example.bank.common.constants.MessageKeys;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
public class DepositPreviewRequest {

    @NotNull(message = "{" + MessageKeys.VALIDATION_DEPOSIT_AMOUNT_NOT_NULL + "}")
    @DecimalMin(
            value = "0.0",
            inclusive = false,
            message = "{" + MessageKeys.VALIDATION_DEPOSIT_AMOUNT_MIN + "}"
    )
    private BigDecimal amount;
}