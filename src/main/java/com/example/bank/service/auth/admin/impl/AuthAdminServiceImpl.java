package com.example.bank.service.auth.admin.impl;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.config.security.jwt.JwtTokenUtils;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.constants.RedisKeys;
import com.example.bank.common.context.AuthContext;
import com.example.bank.common.exception.auth.ForbiddenLoginException;
import com.example.bank.common.exception.auth.OtpException;
import com.example.bank.common.exception.auth.UnauthorizedException;
import com.example.bank.dto.request.auth.AdminLoginOtpRequest;
import com.example.bank.dto.response.auth.TokenResponse;
import com.example.bank.entity.auth.AuthSession;
import com.example.bank.entity.user.User;
import com.example.bank.enums.auth.AuthFailReason;
import com.example.bank.enums.user.AccountStatus;
import com.example.bank.repository.auth.AuthSessionRepository;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.service.auth.admin.AuthAdminService;
import com.example.bank.service.auth.user.AuthSessionService;
import com.example.bank.service.mail.MailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthAdminServiceImpl implements AuthAdminService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final AuthSessionService authSessionService;
    private final JwtTokenUtils jwtTokenUtils;
    private final StringRedisTemplate redis;
    private final MailService mailService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final long ADMIN_LOGIN_OTP_TTL_SECONDS = 300;      // 5 phút
    private static final long ADMIN_LOGIN_OTP_COOLDOWN_SECONDS = 60;  // 1 phút
    private static final long ADMIN_LOGIN_OTP_HOURLY_LIMIT = 5;
    private static final long ADMIN_LOGIN_IP_HOURLY_LIMIT = 10;

    /**
     * API lấy OTP đăng nhập admin.
     * FE chỉ gửi username.
     * Backend tự check username có phải admin không, rồi gửi OTP về email đã đăng ký.
     * <p>
     * Lưu ý:
     * - Không trả lỗi user không tồn tại / không phải admin ra ngoài.
     * - Tránh bị tool dò tài khoản admin.
     */
    @Override
    @Transactional(readOnly = true)
    public void requestLoginOtp(AdminLoginOtpRequest requestDto, HttpServletRequest request) {
        AuthContext ctx = AuthContext.from(request);
        String normalizedUsername = normalize(requestDto.getUsername());

        Optional<User> userOpt = userRepository.findByUsername(normalizedUsername);

        if (userOpt.isEmpty()) {
            log.warn(
                    "ADMIN-LOGIN-OTP-SKIP reason=USER_NOT_FOUND username={} ip={}",
                    maskUsername(normalizedUsername),
                    ctx.getIp()
            );
            return;
        }

        User user = userOpt.get();

        if (user.getStatus() != AccountStatus.ACTIVE) {
            log.warn(
                    "ADMIN-LOGIN-OTP-SKIP reason=USER_NOT_ACTIVE userId={} ip={}",
                    user.getId(),
                    ctx.getIp()
            );
            return;
        }

        if (!isAdmin(user)) {
            log.warn(
                    "ADMIN-LOGIN-OTP-SKIP reason=NOT_ADMIN userId={} ip={}",
                    user.getId(),
                    ctx.getIp()
            );
            return;
        }

        String idKey = adminLoginOtpIdKey(user.getId());
        String otpKey = RedisKeys.otpValueKey(idKey);
        String cooldownKey = RedisKeys.otpCooldownKey(idKey);
        String hourlyKey = RedisKeys.otpHourlyKey(idKey);
        String ipHourlyKey = RedisKeys.otpIpHourlyKey(ctx.getIp());

        // Chống bấm gửi OTP liên tục
        if (Boolean.TRUE.equals(redis.hasKey(cooldownKey))) {
            Long remain = redis.getExpire(cooldownKey, TimeUnit.SECONDS);

            throw new OtpException(
                    MessageKeys.OTP_COOLDOWN_ACTIVE,
                    remain != null ? remain.intValue() : null
            );
        }

        // Rate limit theo IP
        Long ipCount = redis.opsForValue().increment(ipHourlyKey);
        if (ipCount != null && ipCount == 1) {
            redis.expire(ipHourlyKey, 3600, TimeUnit.SECONDS);
        }

        if (ipCount != null && ipCount > ADMIN_LOGIN_IP_HOURLY_LIMIT) {
            Long remain = redis.getExpire(ipHourlyKey, TimeUnit.SECONDS);

            throw new OtpException(
                    MessageKeys.RATE_LIMIT_NETWORK,
                    remain != null ? remain.intValue() : null
            );
        }

        // Rate limit theo admin account
        Long hourlyCount = redis.opsForValue().increment(hourlyKey);
        if (hourlyCount != null && hourlyCount == 1) {
            redis.expire(hourlyKey, 3600, TimeUnit.SECONDS);
        }

        if (hourlyCount != null && hourlyCount > ADMIN_LOGIN_OTP_HOURLY_LIMIT) {
            Long remain = redis.getExpire(hourlyKey, TimeUnit.SECONDS);

            throw new OtpException(
                    MessageKeys.RATE_LIMIT_HOURLY,
                    remain != null ? remain.intValue() : null
            );
        }

        String otp = generateOtp6();

        redis.opsForValue().set(
                otpKey,
                otp,
                ADMIN_LOGIN_OTP_TTL_SECONDS,
                TimeUnit.SECONDS
        );

        redis.opsForValue().set(
                cooldownKey,
                "1",
                ADMIN_LOGIN_OTP_COOLDOWN_SECONDS,
                TimeUnit.SECONDS
        );

        mailService.sendOtp(user.getEmail(), otp);

        log.info(
                "ADMIN-LOGIN-OTP-SENT userId={} email={} ip={}",
                user.getId(),
                maskEmail(user.getEmail()),
                ctx.getIp()
        );
    }

    /**
     * Đăng nhập admin bằng username + password + otp.
     * Chỉ khi username/password đúng và OTP đúng thì mới tạo session + access token.
     */
    @Override
    @Transactional
    public TokenResponse signin(String username, String password, String otp, HttpServletRequest request) {

        AuthContext ctx = AuthContext.from(request);
        String normalizedUsername = normalize(username);
        String normalizedOtp = otp == null ? "" : otp.trim();

        // Xác thực username/password trước
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedUsername, password)
            );
        } catch (AuthenticationException ex) {
            AuthFailReason reason = mapReason(ex);
            String messageKey = mapFailReasonToMessageKey(reason);

            log.warn(
                    "ADMIN-LOGIN-FAIL username={}, ip={}, userAgent={}, reason={}",
                    maskUsername(normalizedUsername),
                    ctx.getIp(),
                    ctx.getUserAgent(),
                    reason.name()
            );

            throw new UnauthorizedException(
                    messageKey,
                    HttpStatus.UNAUTHORIZED
            );
        }

        UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> {
                    log.error(
                            "AUTH-INTEGRITY-ERROR principal authenticated but user not found, userId={}",
                            principal.getId()
                    );

                    return new UnauthorizedException(
                            MessageKeys.AUTHENTICATED_USER_NOT_FOUND,
                            HttpStatus.UNAUTHORIZED
                    );
                });

        // Validate account + role admin
        validateLoginPolicy(user, principal, ctx, normalizedUsername);

        // Check OTP sau khi password đúng
        String idKey = adminLoginOtpIdKey(user.getId());
        String otpKey = RedisKeys.otpValueKey(idKey);

        String cachedOtp = redis.opsForValue().get(otpKey);

        if (cachedOtp == null) {
            log.warn(
                    "ADMIN-LOGIN-FAIL reason=OTP_EXPIRED userId={} ip={}",
                    user.getId(),
                    ctx.getIp()
            );

            throw new UnauthorizedException(
                    MessageKeys.ADMIN_LOGIN_OTP_EXPIRED_OR_NOT_FOUND,
                    HttpStatus.UNAUTHORIZED
            );
        }

        if (!cachedOtp.equals(normalizedOtp)) {
            log.warn(
                    "ADMIN-LOGIN-FAIL reason=OTP_INVALID userId={} ip={}",
                    user.getId(),
                    ctx.getIp()
            );

            throw new UnauthorizedException(
                    MessageKeys.OTP_INVALID,
                    HttpStatus.UNAUTHORIZED
            );
        }

        // OTP đúng mới tạo session + token
        AuthSession session = authSessionService.createSessionBeforeIssueToken(
                principal,
                AuthContext.resolveClientIp(request),
                ctx.getUserAgent()
        );

        TokenResponse tokenResponse = generateTokenResponse(principal, session);

        // OTP dùng xong xoá ngay, tránh reuse
        redis.delete(otpKey);

        log.info(
                "ADMIN-LOGIN-SUCCESS username={}, userId={}, ip={}, userAgent={}",
                maskUsername(normalizedUsername),
                user.getId(),
                ctx.getIp(),
                ctx.getUserAgent()
        );

        return tokenResponse;
    }

    private String adminLoginOtpIdKey(Long userId) {
        return "admin-login:user:" + userId;
    }

    private String generateOtp6() {
        return String.valueOf(SECURE_RANDOM.nextInt(900000) + 100000);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private boolean isAdmin(User user) {
        return user.getRole() != null
                && "ADMIN".equalsIgnoreCase(user.getRole().getName());
    }

    private String mapFailReasonToMessageKey(AuthFailReason reason) {
        return switch (reason) {
            case WRONG_CREDENTIALS -> MessageKeys.WRONG_CREDENTIALS;
            case ACCOUNT_LOCKED -> MessageKeys.USER_ACCOUNT_LOCKED;
            default -> MessageKeys.AUTHENTICATION_FAILED;
        };
    }

    private AuthFailReason mapReason(AuthenticationException ex) {
        if (ex instanceof LockedException) return AuthFailReason.ACCOUNT_LOCKED;
        if (ex instanceof CredentialsExpiredException) return AuthFailReason.CREDS_EXPIRED;
        if (ex instanceof BadCredentialsException) return AuthFailReason.WRONG_CREDENTIALS;
        if (ex instanceof InsufficientAuthenticationException) return AuthFailReason.INSUFFICIENT_AUTH;
        return AuthFailReason.AUTH_FAILURE;
    }

    private void validateLoginPolicy(
            User user,
            UserDetailsImpl principal,
            AuthContext ctx,
            String username
    ) {
        if (user.getStatus() == AccountStatus.DELETED) {
            auditAndDeny(
                    ctx,
                    username,
                    AuthFailReason.ACCOUNT_DELETED,
                    MessageKeys.USER_ACCOUNT_DELETED
            );
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
            auditAndDeny(
                    ctx,
                    username,
                    AuthFailReason.ACCESS_DENIED,
                    MessageKeys.USER_ACCOUNT_NOT_ACTIVE
            );
        }

        if (!principal.isAdmin()) {
            auditAndDeny(
                    ctx,
                    username,
                    AuthFailReason.ACCESS_DENIED,
                    MessageKeys.ACCESS_DENIED
            );
        }
    }

    private void auditAndDeny(
            AuthContext ctx,
            String username,
            AuthFailReason reason,
            String messageKey
    ) {
        log.warn(
                "ADMIN-LOGIN-FAIL username={}, ip={}, ua={}, reason={}",
                maskUsername(username),
                ctx.getIp(),
                ctx.getUserAgent(),
                reason.name()
        );

        throw new ForbiddenLoginException(messageKey);
    }

    private TokenResponse generateTokenResponse(
            UserDetailsImpl principal,
            AuthSession session
    ) {
        String refreshToken = jwtTokenUtils.generateRefreshToken(principal, session.getId());

        authSessionService.attachRefreshTokenHash(session.getId(), refreshToken);

        String accessToken = jwtTokenUtils.generateAccessToken(principal);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private String maskUsername(String username) {
        if (username == null || username.isBlank()) {
            return "unknown";
        }

        if (username.length() <= 3) {
            return "***";
        }

        return username.substring(0, username.length() - 3) + "***";
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }

        int at = email.indexOf("@");

        if (at <= 2) {
            return "***" + email.substring(at);
        }

        return email.substring(0, 2) + "***" + email.substring(at);
    }
}

