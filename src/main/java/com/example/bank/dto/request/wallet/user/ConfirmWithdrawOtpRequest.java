package com.example.bank.dto.request.wallet.user;

import com.example.bank.common.constants.MessageKeys;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmWithdrawOtpRequest {

    @NotBlank(message = "{" + MessageKeys.VALIDATION_ORDER_NO_REQUIRED + "}")
    @Size(max = 50, message = "{" + MessageKeys.VALIDATION_ORDER_NO_MAX_LENGTH + "}")
    private String orderNo;

    @NotBlank(message = "{" + MessageKeys.VALIDATION_OTP_REQUIRED + "}")
    @Pattern(regexp = "^[0-9]{6}$", message = "{" + MessageKeys.VALIDATION_OTP_INVALID + "}")
    private String otp;
}
