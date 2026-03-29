package com.example.bank.dto.request.wallet.user;

import com.example.bank.common.constants.MessageKeys;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.math.BigDecimal;
@Getter
public class TopupCardRequest {
    @NotNull(message = "{" + MessageKeys.VALIDATION_CARD_AMOUNT_REQUIRED + "}")
    @DecimalMin(value = "0.01", message = "{" + MessageKeys.VALIDATION_CARD_AMOUNT_MIN + "}")
    private BigDecimal amount;

    // idempotency (optional nhưng nên có)
    private String referenceId;
}
