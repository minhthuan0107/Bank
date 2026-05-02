package com.example.bank.dto.request.wallet.admin;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.enums.wallet.DepositOrderStatus;
import com.example.bank.enums.wallet.WithdrawOrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateDepositStatusRequest {
    @NotNull(message = "{" + MessageKeys.VALIDATION_STATUS_NOT_NULL + "}")
    @JsonProperty("status")
    private DepositOrderStatus status;

    @Size(max = 3000, message = "{" + MessageKeys.VALIDATION_ADMIN_NOTE_MAX_LENGTH + "}")
    @JsonProperty("admin_note")
    private String adminNote;
}
