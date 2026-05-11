package com.example.bank.controller.user.auth;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.request.auth.SigninRequest;
import com.example.bank.dto.request.auth.SignupRequest;
import com.example.bank.dto.request.wallet.user.ResetPasswordRequest;
import com.example.bank.dto.response.auth.SigninResponse;
import com.example.bank.dto.response.auth.TokenResponse;
import com.example.bank.service.auth.user.impl.AuthServiceImpl;
import com.example.bank.service.otp.ForgotPasswordService;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthServiceImpl authService;
    private final LocalizationUtils i18n;
    private final ForgotPasswordService forgotPasswordService;
    //Api đăng nhập
    @PermitAll
    @PostMapping(value = "/signin")
    public ResponseEntity<SigninResponse> signin(@Valid @RequestBody SigninRequest requestDto,
                                                 HttpServletRequest request) {
        // gọi service xác thực + sinh token
        TokenResponse tokens = authService.signin(requestDto.getUsername(), requestDto.getPassword(), request);
        // trả response thống nhất
        return ResponseEntity.ok(
                SigninResponse.builder()
                        .status(HttpStatus.OK.value())
                        .message(i18n.getLocalizedMessage(MessageKeys.LOGIN_SUCCESS))
                        .tokens(tokens)
                        .build()
        );
    }

    //Api đăng ký
    @PermitAll
    @PostMapping(value = "/signup")
    public ResponseEntity<ApiResponse> signup(@Valid @RequestBody SignupRequest requestDto,
                                              HttpServletRequest request) {
        authService.signup(requestDto, request);
        // Trả về thông báo chuẩn hoá
        return ResponseEntity.ok(ApiResponse.created(
                HttpStatus.CREATED.value(),
                i18n.getLocalizedMessage(MessageKeys.SIGNUP_SUCCESS)));
    }

    // Api làm mới access token
    @PermitAll
    @PostMapping("/refresh-token")
    public ResponseEntity<SigninResponse> refreshAccessToken(@RequestHeader("X-Refresh-Token") String refreshToken,
                                                             HttpServletRequest request
    ) {
        TokenResponse tokens = authService.refreshAccessToken(refreshToken, request);
        // Trả response thống nhất
        return ResponseEntity.ok(
                SigninResponse.builder()
                        .status(HttpStatus.OK.value())
                        .message(i18n.getLocalizedMessage(MessageKeys.SESSION_REFRESHED))
                        .tokens(tokens)
                        .build()
        );
    }

    @PostMapping("/logout")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        authService.logout(currentUser.getId());
        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        null,
                        null
                )
        );
    }

    @PermitAll
    @PostMapping("/reset")
    public ResponseEntity<ApiResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest requestDto
    ) {
        forgotPasswordService.resetPassword(requestDto);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.RESET_PASSWORD_SUCCESS),
                        null
                )
        );
    }

}
