package com.example.bank.dto.request.wallet.infini;

import com.example.bank.common.constants.MessageKeys;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class CreateCardRequest {

    // Reuse the same ID when retrying one application.
    @NotNull(message = "{" + MessageKeys.VALIDATION_CARD_REQUEST_ID_REQUIRED + "}")
    private UUID requestId;

    @NotBlank(message = "{" + MessageKeys.VALIDATION_CARD_NAME_REQUIRED + "}")
    @Size(max = 100, message = "{" + MessageKeys.VALIDATION_CARD_NAME_MAX + "}")
    private String name;

    @NotNull(message = "{" + MessageKeys.VALIDATION_CARD_AMOUNT_REQUIRED + "}")
    @DecimalMin(value = "10.00", message = "{" + MessageKeys.CARD_CREATION_INVALID_AMOUNT + "}")
    @Digits(integer = 15, fraction = 2, message = "{" + MessageKeys.CARD_CREATION_INVALID_AMOUNT + "}")
    private BigDecimal amount;

    @NotBlank(message = "{" + MessageKeys.VALIDATION_CARD_HOLDER_NAME_REQUIRED + "}")
    @Size(max = 100, message = "{" + MessageKeys.VALIDATION_CARD_NAME_MAX + "}")
    private String holderName;

    @Size(max = 255, message = "{" + MessageKeys.VALIDATION_CARD_NOTE_MAX_LENGTH + "}")
    private String note;
}
