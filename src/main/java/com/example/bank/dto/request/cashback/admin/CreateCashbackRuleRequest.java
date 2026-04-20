package com.example.bank.dto.request.cashback.admin;

import com.example.bank.common.constants.MessageKeys;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateCashbackRuleRequest {

    /**
     * Mức chi tiêu tối thiểu
     */
    @NotNull(message = "{" + MessageKeys.VALIDATION_MIN_SPENT_REQUIRED + "}")
    @DecimalMin(value = "0.0", inclusive = true,
            message = "{" + MessageKeys.VALIDATION_MIN_SPENT_INVALID + "}")
    @Digits(integer = 15, fraction = 4,
            message = "{" + MessageKeys.VALIDATION_AMOUNT_FORMAT + "}")
    @JsonProperty("min_spent")
    private BigDecimal minSpent;

    /**
     * Mức chi tiêu tối đa (có thể null)
     */
    @Digits(integer = 15, fraction = 4,
            message = "{" + MessageKeys.VALIDATION_AMOUNT_FORMAT + "}")
    @JsonProperty("max_spent")
    private BigDecimal maxSpent;

    /**
     * % cashback (vd: 1.5 = 1.5%)
     */
    @NotNull(message = "{" + MessageKeys.VALIDATION_CASHBACK_PERCENT_REQUIRED + "}")
    @DecimalMin(value = "0.0", inclusive = false,
            message = "{" + MessageKeys.VALIDATION_CASHBACK_PERCENT_INVALID + "}")
    @Digits(integer = 3, fraction = 2,
            message = "{" + MessageKeys.VALIDATION_PERCENT_FORMAT + "}")
    @JsonProperty("cashback_percent")
    private BigDecimal cashbackPercent;
}