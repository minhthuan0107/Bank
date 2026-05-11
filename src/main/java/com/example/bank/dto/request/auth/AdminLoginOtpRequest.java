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
public class AdminLoginOtpRequest {

    @NotBlank(message = "{" + MessageKeys.VALIDATION_USERNAME_NOT_BLANK + "}")
    @Size(min = 3, max = 50, message = "{" + MessageKeys.VALIDATION_USERNAME_SIZE + "}")
    @Pattern(
            regexp = "^[a-zA-Z0-9._-]+$",
            message = "{" + MessageKeys.VALIDATION_USERNAME_INVALID + "}"
    )
    @JsonProperty("username")
    private String username;
}
