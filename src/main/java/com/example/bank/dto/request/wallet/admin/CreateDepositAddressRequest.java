package com.example.bank.dto.request.wallet.admin;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.enums.wallet.Stablecoin;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDepositAddressRequest {

    @NotNull(message = "{" + MessageKeys.VALIDATION_CURRENCY_NOT_NULL + "}")
    @JsonProperty("currency")
    private Stablecoin currency;

    @NotBlank(message = "{" + MessageKeys.VALIDATION_NETWORK_NOT_BLANK + "}")
    @Size(max = 20, message = "{" + MessageKeys.VALIDATION_NETWORK_MAX_LENGTH + "}")
    @JsonProperty("network")
    private String network;

    @NotBlank(message = "{" + MessageKeys.VALIDATION_DEPOSIT_ADDRESS_NOT_BLANK + "}")
    @Size(max = 255, message = "{" + MessageKeys.VALIDATION_DEPOSIT_ADDRESS_MAX_LENGTH + "}")
    @JsonProperty("address")
    private String address;

    @NotNull(message = "{" + MessageKeys.VALIDATION_DISPLAY_ORDER_NOT_NULL + "}")
    @JsonProperty("display_order")
    private Integer displayOrder;
}
