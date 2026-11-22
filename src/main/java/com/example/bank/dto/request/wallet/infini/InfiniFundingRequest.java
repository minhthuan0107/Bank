package com.example.bank.dto.request.wallet.infini;

import com.example.bank.common.constants.MessageKeys;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** Top-up or withdrawal of an Infini card. */
@Getter
@Setter
public class InfiniFundingRequest {

    // Reuse the same ID when retrying one request.
    @NotNull(message = "{" + MessageKeys.VALIDATION_CARD_REQUEST_ID_REQUIRED + "}")
    private UUID requestId;

    @NotNull(message = "{" + MessageKeys.VALIDATION_CARD_AMOUNT_REQUIRED + "}")
    @DecimalMin(value = "10.00", message = "{" + MessageKeys.INFINI_FUNDING_INVALID_AMOUNT + "}")
    @Digits(integer = 15, fraction = 2, message = "{" + MessageKeys.INFINI_FUNDING_INVALID_AMOUNT + "}")
    private BigDecimal amount;
}
