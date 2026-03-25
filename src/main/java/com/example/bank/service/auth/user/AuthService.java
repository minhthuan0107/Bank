package com.example.bank.service.auth.user;

import com.example.bank.dto.request.auth.SignupRequest;
import com.example.bank.dto.response.auth.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    TokenResponse signin(String username, String password, HttpServletRequest request);

    void signup (SignupRequest request, HttpServletRequest httpRequest);

    TokenResponse refreshAccessToken(String refreshToken, HttpServletRequest request);
}
