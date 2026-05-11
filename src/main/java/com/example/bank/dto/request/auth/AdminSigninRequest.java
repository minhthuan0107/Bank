package com.example.bank.dto.request.auth;

import com.example.bank.common.constants.MessageKeys;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminSigninRequest {

    @NotBlank(message = "{" + MessageKeys.VALIDATION_USERNAME_NOT_BLANK + "}")
    @Size(min = 6, message = "{" + MessageKeys.VALIDATION_USERNAME_MIN_LENGTH + "}")
    @Pattern(
            regexp = "^(?:[0-9]{9,11}|[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+|[A-Za-z0-9_]{6,30})$",
            message = "{" + MessageKeys.VALIDATION_USERNAME_INVALID + "}"
    )
    private String username;

    @NotBlank(message = "{" + MessageKeys.VALIDATION_PASSWORD_NOT_BLANK + "}")
    @JsonProperty("password")
    private String password;

    @NotBlank(message = "{" + MessageKeys.VALIDATION_OTP_NOT_BLANK + "}")
    @Pattern(
            regexp = "^\\d{6}$",
            message = "{" + MessageKeys.VALIDATION_OTP_INVALID + "}"
    )
    @JsonProperty("otp")
    private String otp;
}
