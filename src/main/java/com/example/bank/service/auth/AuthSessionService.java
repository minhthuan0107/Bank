package com.example.bank.service.auth;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.entity.auth.AuthSession;

public interface AuthSessionService {
    /**
     * Tạo phiên đăng nhập trước khi sinh refresh token.
     * Trả về AuthSession có chứa sessionId.
     */
    AuthSession createSessionBeforeIssueToken(
            UserDetailsImpl principal,
            String ip,
            String userAgent
    );

    /**
     * Sau khi sinh refresh token → hash → lưu vào phiên.
     */
    void attachRefreshTokenHash(Long sessionId, String refreshToken);
}
