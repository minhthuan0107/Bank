package com.example.bank.service.auth.admin.impl;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.config.security.jwt.JwtTokenUtils;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.context.AuthContext;
import com.example.bank.common.exception.auth.ForbiddenLoginException;
import com.example.bank.common.exception.auth.UnauthorizedException;
import com.example.bank.common.utils.HashUtils;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.response.auth.TokenResponse;
import com.example.bank.entity.auth.AuthSession;
import com.example.bank.entity.user.Role;
import com.example.bank.entity.user.User;
import com.example.bank.enums.auth.AuthFailReason;
import com.example.bank.enums.user.AccountStatus;
import com.example.bank.repository.auth.AuthSessionRepository;
import com.example.bank.repository.user.RoleRepository;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.service.auth.admin.AuthAdminService;
import com.example.bank.service.auth.user.AuthSessionService;
import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthAdminServiceImpl implements AuthAdminService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final AuthSessionService authSessionService;
    private final JwtTokenUtils jwtTokenUtils;
    private final AuthSessionRepository authSessionRepository;
    //  ĐĂNG NHẬP
    @Override
    @Transactional
    public TokenResponse signin(String username, String password, HttpServletRequest request) {

        AuthContext ctx = AuthContext.from(request);

        // Xác thực username/password
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
        } catch (AuthenticationException ex) {
            // Xác thực thất bại
            AuthFailReason reason = mapReason(ex);
            // Map sang message key
            String messageKey = mapFailReasonToMessageKey(reason);
            // Ghi log thất bại
            log.warn("Login-FAIL: username={}, ip={}, userAgent={},reason={}",
                    maskUsername(username),
                    ctx.getIp(),
                    ctx.getUserAgent(),
                    reason.name()
            );
            // Ném UnauthorizedException
            throw new UnauthorizedException(
                    messageKey,
                    HttpStatus.UNAUTHORIZED
            );
        }

        // Lấy thông tin user
        UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();
        // Load user entity đầy đủ
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> {
                    log.error("AUTH-INTEGRITY-ERROR: principal authenticated but user not found, userId={}", principal.getId());
                    // Ném lỗi
                    return new UnauthorizedException(
                            MessageKeys.AUTHENTICATED_USER_NOT_FOUND,
                            HttpStatus.UNAUTHORIZED);
                });

        // Validate chính sách đăng nhập
        validateLoginPolicy(user, principal, ctx, username);

        // Login success
        log.info("Login-SUCCESS: username={}, ip={}, userAgent={}",
                maskUsername(username),
                ctx.getIp(),
                ctx.getUserAgent()
        );

        // Session & token
        AuthSession session = authSessionService.createSessionBeforeIssueToken(
                principal,
                AuthContext.resolveClientIp(request),
                ctx.getUserAgent()
        );

        // Generate token response
        return generateTokenResponse(principal, session);
    }

    // Map lý do thất bại từ AuthenticationException
    private String mapFailReasonToMessageKey(AuthFailReason reason) {
        return switch (reason) {
            case WRONG_CREDENTIALS -> MessageKeys.WRONG_CREDENTIALS;
            case ACCOUNT_LOCKED -> MessageKeys.USER_ACCOUNT_LOCKED;
            default -> MessageKeys.AUTHENTICATION_FAILED;
        };
    }

    // ================================
    // Chuẩn hoá lỗi
    private AuthFailReason mapReason(AuthenticationException ex) {
        if (ex instanceof LockedException) return AuthFailReason.ACCOUNT_LOCKED;
        if (ex instanceof CredentialsExpiredException) return AuthFailReason.CREDS_EXPIRED;
        if (ex instanceof BadCredentialsException) return AuthFailReason.WRONG_CREDENTIALS;
        if (ex instanceof InsufficientAuthenticationException) return AuthFailReason.INSUFFICIENT_AUTH;
        return AuthFailReason.AUTH_FAILURE;
    }

    // Validate chính sách đăng nhập
    private void validateLoginPolicy(
            User user,
            UserDetailsImpl principal,
            AuthContext ctx,
            String username
    ) {
        // Chặn user bị xóa
        if (user.getStatus() == AccountStatus.DELETED) {
            auditAndDeny(
                    ctx,
                    username,
                    AuthFailReason.ACCOUNT_DELETED,
                    MessageKeys.USER_ACCOUNT_DELETED
            );
        }

        // Chặn admin login kênh user
        if (!principal.isAdmin()) {
            auditAndDeny(
                    ctx,
                    username,
                    AuthFailReason.ACCESS_DENIED,
                    MessageKeys.ACCESS_DENIED
            );
        }
    }

    // Hàm audit và ném ForbiddenLoginException
    private void auditAndDeny(
            AuthContext ctx,
            String username,
            AuthFailReason reason,
            String messageKey
    ) {
        log.warn("Login-FAIL: username={}, ip={}, ua={},reason={}",
                maskUsername(username),
                ctx.getIp(),
                ctx.getUserAgent(),
                reason.name()
        );
        throw new ForbiddenLoginException(messageKey);

    }

    // Sinh token response
    private TokenResponse generateTokenResponse(
            UserDetailsImpl principal,
            AuthSession session
    ) {
        // Sinh refresh token có sid
        String refreshToken = jwtTokenUtils.generateRefreshToken(principal, session.getId());

        // Lưu hash refresh token vào session
        authSessionService.attachRefreshTokenHash(session.getId(), refreshToken);

        // Sinh access token
        String accessToken = jwtTokenUtils.generateAccessToken(principal);

        // Trả về token response
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private String maskUsername(String username) {
        if (username == null || username.isBlank()) {
            return "unknown";
        }
        return username.substring(0, username.length() - 3) + "***";
    }

    // Làm mới access token từ refresh token
    @Override
    @Transactional
    public TokenResponse refreshAccessToken(String refreshToken, HttpServletRequest request) {
        // Validate refresh token không rỗng
        validateRefreshTokenNotBlank(refreshToken, request);

        // Parse & extract claims từ refresh token
        Claims claims = jwtTokenUtils.safeParseRefreshClaims(refreshToken);
        validateRefreshTokenType(claims, request);

        // Lấy thông tin từ claims
        Long userId = Long.valueOf(claims.getSubject());
        Long sessionId = claims.get("sid", Long.class);
        Integer tokenPv = claims.get("pv", Integer.class);

        // Load session (kiểm tra revoked, expired, hash)
        loadAndValidateSession(sessionId, userId, refreshToken);

        // Load user đầy đủ (eager load)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException(
                        MessageKeys.ACCOUNT_NOT_FOUND,
                        HttpStatus.UNAUTHORIZED));

        // Kiểm tra user status (lock/disabled/deleted)
        ensureUserAllowedToLogin(user);

        // Kiểm tra password version (nếu user đổi mật khẩu, token cũ không dùng được)
        if (!tokenPv.equals(user.getPasswordVersion())) {
            log.warn("AUTH-REFRESH-FAIL: password version mismatch, userId={}", userId);
            throw new UnauthorizedException(
                    MessageKeys.SESSION_INVALID,
                    HttpStatus.UNAUTHORIZED);
        }
        // Cập nhật lastUsedAt của session
        authSessionRepository.updateLastUsedAtById(sessionId, Instant.now());
        // Sinh access token mới
        UserDetailsImpl principal = UserDetailsImpl.from(user);
        String accessToken = jwtTokenUtils.generateAccessToken(principal);

        log.info("AUTH-REFRESH-SUCCESS: userId={}, sid={}", userId, sessionId);
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // ================================
    // Validate refresh token không rỗng
    private void validateRefreshTokenNotBlank(String refreshToken, HttpServletRequest request) {
        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("AUTH-REFRESH-FAIL: token missing, ip={}", AuthContext.resolveClientIp(request));
            throw new UnauthorizedException(
                    MessageKeys.SESSION_INVALID,
                    HttpStatus.UNAUTHORIZED);
        }
    }

    // Validate token type = refresh
    private void validateRefreshTokenType(Claims claims, HttpServletRequest request) {
        if (!"refresh".equals(claims.get("typ"))) {
            log.warn("AUTH-REFRESH-FAIL: typ!=refresh, ip={}", AuthContext.resolveClientIp(request));
            throw new UnauthorizedException(
                    MessageKeys.SESSION_INVALID,
                    HttpStatus.UNAUTHORIZED);
        }
    }

    // Load & validate session (kiểm tra revoke, expire, hash)
    private AuthSession loadAndValidateSession(Long sessionId, Long userId, String refreshToken) {
        // Load session
        AuthSession session = authSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> {
                    log.warn("AUTH-REFRESH-FAIL: session not found, sid={}, userId={}", sessionId, userId);
                    return new UnauthorizedException(
                            MessageKeys.SESSION_INVALID,
                            HttpStatus.UNAUTHORIZED);
                });

        // Kiểm tra session bị revoke
        if (Boolean.TRUE.equals(session.getIsRevoked())) {
            log.warn("AUTH-REFRESH-FAIL: session revoked, sid={}, userId={}", sessionId, userId);
            throw new UnauthorizedException(
                    MessageKeys.SESSION_INVALID,
                    HttpStatus.UNAUTHORIZED);
        }

        // Kiểm tra session expired
        if (session.getExpiresAt().isBefore(Instant.now())) {
            log.warn("AUTH-REFRESH-FAIL: session expired, sid={}, userId={}", sessionId, userId);
            throw new UnauthorizedException(
                    MessageKeys.SESSION_EXPIRED,
                    HttpStatus.UNAUTHORIZED);
        }

        // Kiểm tra hash refresh token khớp
        String expectedHash = session.getRefreshTokenHash();
        String actualHash = HashUtils.sha256Hex(refreshToken);
        if (expectedHash == null || actualHash == null || !expectedHash.equals(actualHash)) {
            log.warn("AUTH-REFRESH-FAIL: invalid refresh token, sid={}, userId={}", sessionId, userId);
            throw new UnauthorizedException(
                    MessageKeys.SESSION_INVALID,
                    HttpStatus.UNAUTHORIZED);
        }
        return session;
    }

    //Check trạng thái user trước khi login bằng OAuth
    private void ensureUserAllowedToLogin(User user) {
        if (user.getStatus() == AccountStatus.DELETED) {
            log.warn("AUTH-BLOCKED reason=DELETED userId={}", user.getId());
            throw new UnauthorizedException(
                    MessageKeys.USER_ACCOUNT_DELETED,
                    HttpStatus.UNAUTHORIZED);
        }
        if (user.getStatus() == AccountStatus.LOCKED) {
            log.warn("AUTH-BLOCKED reason=LOCKED userId={}", user.getId());
            throw new UnauthorizedException(
                    MessageKeys.USER_ACCOUNT_LOCKED,
                    HttpStatus.UNAUTHORIZED);
        }
    }



}
