package com.example.bank.service.auth.impl;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.config.security.jwt.JwtProperties;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.auth.UnauthorizedException;
import com.example.bank.common.utils.HashUtils;
import com.example.bank.entity.auth.AuthSession;
import com.example.bank.entity.user.User;
import com.example.bank.repository.auth.AuthSessionRepository;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.service.auth.AuthSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthSessionServiceImpl implements AuthSessionService {

    private final AuthSessionRepository repository;
    private final UserRepository userRepository;
    private final JwtProperties properties;

    /**
     *Tạo phiên trống trước khi sinh refresh token (để lấy sessionId)
     */
    @Override
    public AuthSession createSessionBeforeIssueToken(
            UserDetailsImpl principal,
            String ip,
            String userAgent
    ) {
       Instant now = Instant.now();

        User userRef = userRepository.getReferenceById(principal.getId());
        // Tạo phiên mới
        AuthSession session = AuthSession.builder()
                .user(userRef)
                .ipAddress(emptyToNull(ip))
                .userAgent(truncate(userAgent, 255))
                .issuedAt(now)
                .expiresAt(now.plus(properties.getRefreshTtl()))    // TTL refresh token (7 ngày hoặc theo config)
                .lastUsedAt(now)
                .isRevoked(false)
                .build();
        // Lưu phiên vào cơ sở dữ liệu
        session = repository.save(session);
        log.info("[SESSION][CREATE] userId={}, sessionId={}, deviceId={}, ip={}",
                principal.getId(),
                session.getId(),
                ip);

        return session;
    }

    /**
     *Sau khi đã sinh refresh token → hash → lưu vào session
     */
    @Override
    public void attachRefreshTokenHash(Long sessionId, String refreshToken) {
        AuthSession session = repository.findById(sessionId)
                .orElseThrow(() -> new UnauthorizedException(
                        MessageKeys.AUTH_SESSION_NOT_FOUND,
                        HttpStatus.UNAUTHORIZED));

        // Tạo hash của refresh token
        String hash = HashUtils.sha256Hex(refreshToken);
        session.setRefreshTokenHash(hash);
        repository.save(session);
    }


    // =======================================
    // Chuyển chuỗi rỗng hoặc chỉ chứa khoảng trắng thành null
    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
    // Cắt chuỗi nếu vượt quá độ dài tối đa
    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
