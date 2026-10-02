package com.example.bank.dto.request.wallet.user;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.enums.wallet.CryptoNetwork;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateWalletExternalAddressRequest {

    @NotNull(
            message = "{" + MessageKeys.VALIDATION_WALLET_EXTERNAL_ADDRESS_NETWORK_REQUIRED + "}"
    )
    private CryptoNetwork network;

    @NotBlank(
            message = "{" + MessageKeys.VALIDATION_WALLET_EXTERNAL_ADDRESS_NOT_BLANK + "}"
    )
    private String address;
}
