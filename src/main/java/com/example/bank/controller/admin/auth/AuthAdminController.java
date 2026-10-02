package com.example.bank.controller.admin.auth;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.request.auth.AdminLoginOtpRequest;
import com.example.bank.dto.request.auth.AdminSigninRequest;
import com.example.bank.dto.request.auth.SigninRequest;
import com.example.bank.dto.response.auth.SigninResponse;
import com.example.bank.dto.response.auth.TokenResponse;
import com.example.bank.service.auth.admin.AuthAdminService;
import com.example.bank.service.auth.user.impl.AuthServiceImpl;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/admin/auth")
@RequiredArgsConstructor
public class AuthAdminController {
    private final AuthAdminService adminAuthService;
    private final LocalizationUtils i18n;

    /**
     * API lấy OTP đăng nhập admin.
     * FE gửi username, backend tự lấy email admin để gửi OTP.
     */
    @PermitAll
    @PostMapping("/request-login-otp")
    public ResponseEntity<ApiResponse> requestAdminLoginOtp(
            @Valid @RequestBody AdminLoginOtpRequest requestDto,
            HttpServletRequest request
    ) {
        adminAuthService.requestLoginOtp(requestDto, request);

        return ResponseEntity.ok(
                ApiResponse.ok(
                        HttpStatus.OK.value(),
                        null
                )
        );
    }


    /**
     * API đăng nhập admin bằng username + password + OTP.
     */
    @PermitAll
    @PostMapping("/signin")
    public ResponseEntity<SigninResponse> adminSignin(
            @Valid @RequestBody AdminSigninRequest requestDto,
            HttpServletRequest request
    ) {
        TokenResponse tokens = adminAuthService.signin(
                requestDto.getUsername(),
                requestDto.getPassword(),
                requestDto.getOtp(),
                request
        );

        return ResponseEntity.ok(
                SigninResponse.builder()
                        .status(HttpStatus.OK.value())
                        .message(i18n.getLocalizedMessage(MessageKeys.LOGIN_SUCCESS))
                        .tokens(tokens)
                        .build()
        );
    }
}
