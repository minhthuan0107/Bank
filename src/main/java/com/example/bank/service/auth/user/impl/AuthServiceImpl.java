package com.example.bank.service.auth.user.impl;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.config.security.jwt.JwtTokenUtils;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.constants.RedisKeys;
import com.example.bank.common.context.AuthContext;
import com.example.bank.common.exception.auth.AuthException;
import com.example.bank.common.exception.auth.ForbiddenLoginException;
import com.example.bank.common.exception.auth.OtpException;
import com.example.bank.common.exception.auth.UnauthorizedException;
import com.example.bank.common.utils.HashUtils;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.request.auth.SignupRequest;
import com.example.bank.dto.response.auth.TokenResponse;
import com.example.bank.entity.auth.AuthSession;
import com.example.bank.entity.user.Role;
import com.example.bank.entity.user.User;
import com.example.bank.enums.auth.AuthFailReason;
import com.example.bank.enums.user.AccountStatus;
import com.example.bank.repository.auth.AuthSessionRepository;
import com.example.bank.repository.user.RoleRepository;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.service.auth.user.AuthService;
import com.example.bank.service.auth.user.AuthSessionService;
import com.example.bank.service.wallet.admin.WalletService;
import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final RoleRepository roleRepository;
    private final LocalizationUtils i18n;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final AuthSessionService authSessionService;
    private final JwtTokenUtils jwtTokenUtils;
    private Role defaultUserRole;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final WalletService walletService;
    private final AuthSessionRepository authSessionRepository;
    @PostConstruct
    public void init() {
        defaultUserRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException(
                        i18n.getLocalizedMessage(MessageKeys.ROLE_DEFAULT_NOT_FOUND)
                ));
    }

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
        return generateTokenResponse(principal,session);
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

    // ================================
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
        if (principal.isAdmin()) {
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
            String username ,
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
    // ĐĂNG KÝ
    @Override
    @Transactional
    public void signup(SignupRequest request, HttpServletRequest httpRequest) {

        AuthContext ctx = AuthContext.from(httpRequest);

        // ===== Normalize =====
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();
        String otp = request.getOtp().trim();

        String idKey = "email:" + email;
        String otpKey = RedisKeys.otpValueKey(idKey);

        // ===== GET OTP FROM REDIS =====
        String cachedOtp = redisTemplate.opsForValue().get(otpKey);

        if (cachedOtp == null) {
            log.warn("AUTH-SIGNUP-FAIL: email={}, ip={}, reason={}",
                    maskEmail(email),
                    ctx.getIp(),
                    "OTP_EXPIRED_OR_NOT_FOUND"
            );

            throw new OtpException(MessageKeys.OTP_EXPIRED_OR_NOT_FOUND);
        }

        if (!cachedOtp.equals(otp)) {

            log.warn("AUTH-SIGNUP-FAIL: email={}, ip={}, reason={}",
                    maskEmail(email),
                    ctx.getIp(),
                    "OTP_INVALID"
            );

            throw new OtpException(MessageKeys.OTP_INVALID);
        }

        // ===== DELETE OTP (avoid reuse) =====
        redisTemplate.delete(otpKey);

        // ===== CHECK USERNAME =====
        if (userRepository.existsByUsername(username)) {
            log.warn("AUTH-SIGNUP-FAIL: username={}, ip={}, reason={}",
                    maskUsername(username),
                    ctx.getIp(),
                    "ACCOUNT_EXISTS"
            );

            throw new AuthException(
                    MessageKeys.ACCOUNT_ALREADY_EXISTS,
                    HttpStatus.CONFLICT
            );
        }

        // ===== CHECK EMAIL =====
        if (userRepository.existsByEmail(email)) {
            log.warn("AUTH-SIGNUP-FAIL: email={}, ip={}, reason={}",
                    maskEmail(email),
                    ctx.getIp(),
                    "EMAIL_ALREADY_EXISTS"
            );

            throw new AuthException(
                    MessageKeys.EMAIL_ALREADY_EXISTS,
                    HttpStatus.CONFLICT
            );
        }

        // ===== PASSWORD VALIDATE =====
        if (!request.isPasswordMatched()) {

            log.warn("AUTH-SIGNUP-FAIL: username={}, ip={}, reason={}",
                    maskUsername(username),
                    ctx.getIp(),
                    "PASSWORD_MISMATCH"
            );

            throw new AuthException(
                    MessageKeys.VALIDATION_PASSWORD_MISMATCH,
                    HttpStatus.BAD_REQUEST
            );
        }

        // ===== CREATE USER =====
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .passwordVersion(1)
                .role(defaultUserRole)
                .status(AccountStatus.ACTIVE)
                .build();

        try {

            userRepository.save(user);

            //Tạo wallet cho user
            walletService.createWallet(user.getId());

            log.info("AUTH-SIGNUP-SUCCESS: username={}, email={}, ip={}",
                    maskUsername(username),
                    maskEmail(email),
                    ctx.getIp()
            );

        } catch (DataIntegrityViolationException ex) {

            log.warn("AUTH-SIGNUP-FAIL (DB UNIQUE): username={}, email={}, ip={}",
                    maskUsername(username),
                    maskEmail(email),
                    ctx.getIp()
            );

            throw new AuthException(
                    MessageKeys.ACCOUNT_ALREADY_EXISTS,
                    HttpStatus.CONFLICT
            );
        }
    }

    private String maskEmail(String email) {
        int at = email.indexOf("@");
        if (at <= 2) return email;

        return email.substring(0, 2)
                + "***"
                + email.substring(at);
    }

    /**
     * Làm mới access token từ refresh token
     */
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
