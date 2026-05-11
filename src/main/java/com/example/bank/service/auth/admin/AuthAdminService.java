package com.example.bank.service.auth.admin;

import com.example.bank.dto.request.auth.AdminLoginOtpRequest;
import com.example.bank.dto.response.auth.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthAdminService {
    void requestLoginOtp(AdminLoginOtpRequest requestDto, HttpServletRequest request);

    TokenResponse signin(String username, String password, String otp, HttpServletRequest request);

}
