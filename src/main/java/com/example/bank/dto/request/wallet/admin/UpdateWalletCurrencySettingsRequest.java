package com.example.bank.dto.request.wallet.admin;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.enums.wallet.Stablecoin;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateWalletCurrencySettingsRequest {

    @NotNull(message = "{" + MessageKeys.VALIDATION_CURRENCY_NOT_NULL + "}")
    @JsonProperty("currency")
    private Stablecoin currency;

    @NotNull(message = "{" + MessageKeys.VALIDATION_DEPOSIT_FEE_PERCENT_NOT_NULL + "}")
    @DecimalMin(value = "0.0", message = "{" + MessageKeys.VALIDATION_DEPOSIT_FEE_PERCENT_MIN + "}")
    @JsonProperty("deposit_fee_percent")
    private BigDecimal depositFeePercent;

    @NotNull(message = "{" + MessageKeys.VALIDATION_MIN_DEPOSIT_AMOUNT_NOT_NULL + "}")
    @DecimalMin(value = "0.0", inclusive = false, message = "{" + MessageKeys.VALIDATION_MIN_DEPOSIT_AMOUNT_MIN + "}")
    @JsonProperty("min_deposit_amount")
    private BigDecimal minDepositAmount;

    @NotNull(message = "{" + MessageKeys.VALIDATION_MIN_WITHDRAW_AMOUNT_NOT_NULL + "}")
    @DecimalMin(value = "0.0", inclusive = false, message = "{" + MessageKeys.VALIDATION_MIN_WITHDRAW_AMOUNT_MIN + "}")
    @JsonProperty("min_withdraw_amount")
    private BigDecimal minWithdrawAmount;

    @NotNull(message = "{" + MessageKeys.VALIDATION_MIN_CARD_FUNDING_AMOUNT_NOT_NULL + "}")
    @DecimalMin(value = "0.0", inclusive = false, message = "{" + MessageKeys.VALIDATION_MIN_CARD_FUNDING_AMOUNT_MIN + "}")
    @JsonProperty("min_card_funding_amount")
    private BigDecimal minCardFundingAmount;

}