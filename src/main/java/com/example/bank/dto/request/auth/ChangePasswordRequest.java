package com.example.bank.dto.request.auth;

import com.example.bank.common.constants.MessageKeys;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChangePasswordRequest {

    @NotBlank(message = "{" + MessageKeys.VALIDATION_CURRENT_PASSWORD_NOT_BLANK + "}")
    @JsonProperty("current_password")
    private String currentPassword;

    @NotBlank(message = "{" + MessageKeys.VALIDATION_NEW_PASSWORD_NOT_BLANK + "}")
    @Size(min = 6, max = 100, message = "{" + MessageKeys.VALIDATION_PASSWORD_LENGTH + "}")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).{6,100}$",
            message = "{" + MessageKeys.VALIDATION_PASSWORD_COMPLEXITY + "}"
    )
    @JsonProperty("new_password")
    private String newPassword;

    @NotBlank(message = "{" + MessageKeys.VALIDATION_CONFIRM_NEW_PASSWORD_NOT_BLANK + "}")
    @JsonProperty("confirm_new_password")
    private String confirmNewPassword;

    /**
     * Kiểm tra mật khẩu mới và xác nhận mật khẩu mới có trùng nhau không.
     */
    public boolean isNewPasswordMatched() {
        return newPassword != null && newPassword.equals(confirmNewPassword);
    }
}