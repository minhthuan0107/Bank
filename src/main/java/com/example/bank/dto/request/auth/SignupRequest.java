package com.example.bank.dto.request.auth;

import com.example.bank.common.constants.MessageKeys;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Builder
@AllArgsConstructor
public class SignupRequest {
    @NotBlank(message = "{" + MessageKeys.VALIDATION_USERNAME_NOT_BLANK + "}")
    @Pattern(
            regexp = "^(?:[0-9]{9,11}|[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+|[A-Za-z0-9_]{6,30})$",
            message = "{" + MessageKeys.VALIDATION_USERNAME_INVALID + "}"
    )
    private String username;

    @NotBlank(message = "{" + MessageKeys.VALIDATION_EMAIL_NOT_BLANK + "}")
    @Email(message = "{" + MessageKeys.VALIDATION_EMAIL_INVALID + "}")
    private String email;


    @NotBlank(message = "{" + MessageKeys.VALIDATION_PASSWORD_NOT_BLANK + "}")
    @Size(min = 6, max = 100, message = "{" + MessageKeys.VALIDATION_PASSWORD_LENGTH + "}")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).{6,100}$",
            message = "{" + MessageKeys.VALIDATION_PASSWORD_COMPLEXITY + "}"
    )
    private String password; // mật khẩu

    @NotBlank(message = "{" + MessageKeys.VALIDATION_CONFIRM_PASSWORD_NOT_BLANK + "}")
    private String confirmPassword;

    @NotBlank(message = "{" + MessageKeys.VALIDATION_OTP_REQUIRED + "}")
    private String otp;// nhập lại mật khẩu

    /**
     * Kiểm tra hai mật khẩu có trùng nhau không.
     */
    public boolean isPasswordMatched() {
        return password != null && password.equals(confirmPassword);
    }


}
