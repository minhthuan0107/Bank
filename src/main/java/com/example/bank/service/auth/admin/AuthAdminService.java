package com.example.bank.service.auth.admin;

import com.example.bank.dto.response.auth.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthAdminService {
    TokenResponse signin(String username, String password, HttpServletRequest request);

    TokenResponse refreshAccessToken(String refreshToken, HttpServletRequest request);
}
