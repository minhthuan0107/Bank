package com.example.bank.dto.request.wallet.admin;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.enums.wallet.Stablecoin;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

    @Getter
    @Setter
    public class CreateDepositSettingsRequest {

        @NotNull(message = "{" + MessageKeys.VALIDATION_CURRENCY_NOT_BLANK + "}")
        @Size(max = 10, message = "{" + MessageKeys.VALIDATION_CURRENCY_MAX_LENGTH + "}")
        @JsonProperty("currency")
        private Stablecoin currency;

        @NotNull(message = "{" + MessageKeys.VALIDATION_FEE_PERCENT_NOT_NULL + "}")
        @DecimalMin(value = "0.0", message = "{" + MessageKeys.VALIDATION_FEE_PERCENT_MIN + "}")
        @DecimalMax(value = "100.0", message = "{" + MessageKeys.VALIDATION_FEE_PERCENT_MAX + "}")
        @JsonProperty("fee_percent")
        private BigDecimal feePercent;

        @NotNull(message = "{" + MessageKeys.VALIDATION_MIN_AMOUNT_NOT_NULL + "}")
        @DecimalMin(value = "0.0", message = "{" + MessageKeys.VALIDATION_MIN_AMOUNT_MIN + "}")
        @JsonProperty("min_amount")
        private BigDecimal minAmount;

        @DecimalMin(value = "0.0", message = "{" + MessageKeys.VALIDATION_MAX_AMOUNT_MIN + "}")
        @JsonProperty("max_amount")
        private BigDecimal maxAmount;

        @AssertTrue(message = "{" + MessageKeys.VALIDATION_MAX_AMOUNT_GREATER_THAN_MIN + "}")
        public boolean isMaxAmountValid() {
            if (maxAmount == null || minAmount == null) {
                return true;
            }
            return maxAmount.compareTo(minAmount) >= 0;
        }
    }


