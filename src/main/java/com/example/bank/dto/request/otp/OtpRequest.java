package com.example.bank.dto.request.otp;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.enums.otp.OtpPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OtpRequest {

    @NotBlank(message = "{" + MessageKeys.VALIDATION_EMAIL_NOT_BLANK + "}")
    @Email(message = "{" + MessageKeys.VALIDATION_EMAIL_INVALID + "}")
    @Size(max = 255)
    private String email;

    @NotNull(message = "{" + MessageKeys.VALIDATION_OTP_PURPOSE_NOT_NULL + "}")
    private OtpPurpose purpose;   // SIGNUP | SIGNIN | RESET
}
