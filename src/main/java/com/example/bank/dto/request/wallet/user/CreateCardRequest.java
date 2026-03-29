package com.example.bank.dto.request.wallet.user;

import com.example.bank.common.constants.MessageKeys;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateCardRequest {

    @NotBlank(message = "{" + MessageKeys.VALIDATION_CARD_BIN_REQUIRED + "}")
    private String bin;

    @NotBlank(message = "{" + MessageKeys.VALIDATION_CARD_NAME_REQUIRED + "}")
    private String name;

    @NotNull(message = "{" + MessageKeys.VALIDATION_CARD_AMOUNT_REQUIRED + "}")
    @DecimalMin(value = "0.01", message = "{" + MessageKeys.VALIDATION_CARD_AMOUNT_MIN + "}")
    private BigDecimal amount;

    @NotNull
    @Valid
    private CardHolderRequest holder;

    @Getter
    @Setter
    public static class CardHolderRequest {

        @NotBlank(message = "{" + MessageKeys.VALIDATION_FIRST_NAME_REQUIRED + "}")
        private String firstName;

        @NotBlank(message = "{" + MessageKeys.VALIDATION_LAST_NAME_REQUIRED + "}")
        private String lastName;

        @NotBlank(message = "{" + MessageKeys.VALIDATION_ADDRESS_REQUIRED + "}")
        private String addressLine;

        @NotBlank(message = "{" + MessageKeys.VALIDATION_CITY_REQUIRED + "}")
        private String city;

        @NotBlank(message = "{" + MessageKeys.VALIDATION_STATE_REQUIRED + "}")
        private String state;

        @NotBlank(message = "{" + MessageKeys.VALIDATION_POSTAL_CODE_REQUIRED + "}")
        private String postalCode;

        @NotBlank(message = "{" + MessageKeys.VALIDATION_COUNTRY_REQUIRED + "}")
        private String country;
    }
}
