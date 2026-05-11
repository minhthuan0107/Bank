package com.example.bank.dto.request.wallet.user;

import com.example.bank.common.constants.MessageKeys;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "{" + MessageKeys.VALIDATION_USERNAME_NOT_BLANK + "}")
    @Size(min = 3, max = 50, message = "{" + MessageKeys.VALIDATION_USERNAME_SIZE + "}")
    @Pattern(
            regexp = "^[a-zA-Z0-9._-]+$",
            message = "{" + MessageKeys.VALIDATION_USERNAME_INVALID + "}"
    )
    @JsonProperty("username")
    private String username;

    @NotBlank(message = "{" + MessageKeys.VALIDATION_OTP_NOT_BLANK + "}")
    @Pattern(
            regexp = "^\\d{6}$",
            message = "{" + MessageKeys.VALIDATION_OTP_INVALID + "}"
    )
    @JsonProperty("otp")
    private String otp;

    @NotBlank(message = "{" + MessageKeys.VALIDATION_NEW_PASSWORD_NOT_BLANK + "}")
    @Size(min = 8, max = 72, message = "{" + MessageKeys.VALIDATION_PASSWORD_SIZE + "}")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "{" + MessageKeys.VALIDATION_PASSWORD_WEAK + "}"
    )
    @JsonProperty("new_password")
    private String newPassword;

    @NotBlank(message = "{" + MessageKeys.VALIDATION_CONFIRM_NEW_PASSWORD_NOT_BLANK + "}")
    @JsonProperty("confirm_new_password")
    private String confirmNewPassword;
}