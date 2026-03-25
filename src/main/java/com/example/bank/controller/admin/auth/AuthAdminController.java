package com.example.bank.controller.admin.auth;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.utils.LocalizationUtils;
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
    private final AuthAdminService authService;
    private final LocalizationUtils i18n;

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
}
