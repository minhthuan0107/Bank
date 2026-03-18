package com.example.bank.dto.request.wallet.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmWithdrawOtpRequest {

    @NotNull
    private String orderNo;

    @NotBlank
    private String otp;
}
