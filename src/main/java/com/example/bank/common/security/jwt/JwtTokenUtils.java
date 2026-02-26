package com.example.bank.common.security.jwt;

import com.example.angelproject.common.config.constants.MessageKeys;
import com.example.angelproject.common.context.enums.AuthChannel;
import com.example.angelproject.common.exception.auth.JwtAuthenticationException;
import com.example.angelproject.modules.auth.application.model.sercurity.AuthPrincipal;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
/*
    Utility class để tạo và xác thực JWT tokens (access + refresh)
 */
public class JwtTokenUtils {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenUtils.class);

    private final JwtProperties properties;

    private Key accessKey() {
        return new SecretKeySpec(Base64.getDecoder().decode(properties.getSecret()), "HmacSHA256");
    }

    private Key refreshKey() {
        return new SecretKeySpec(Base64.getDecoder().decode(properties.getRefreshSecret()), "HmacSHA256");
    }

    private Key resetKey() {
        return new SecretKeySpec(Base64.getDecoder().decode(properties.getResetSecret()), "HmacSHA256");
    }

    /**
     * ACCESS: 15m (theo cấu hình), chứa roles/pv/chn, KHÔNG PII.
     */
    public String generateAccessToken( AuthPrincipal p,  AuthChannel channel) {
        String role = p.admin() ? "ADMIN" : "USER";
        Instant now = Instant.now();

        JwtBuilder b = Jwts.builder()
                .setHeaderParam("typ", "JWT") // header
                .setIssuer(properties.getIssuer())
                .setSubject(String.valueOf(p.userId()))         // sub = userId
                .setId(UUID.randomUUID().toString())           // jti (optional cho access)
                .claim("typ", "access")                        // đánh dấu loại token
                .claim("role", role)                         // dùng số nhiều cho rõ nghĩa
                .claim("pv", p.passwordVersion())           // password_version để revoke
                .claim("chn", channel.name())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(properties.getAccessTtl())))
                .signWith(accessKey(), SignatureAlgorithm.HS256);

        return b.compact();
    }

    /**
     * REFRESH: 7d (theo cấu hình), KHÔNG chứa PII/roles.
     */
    public String generateRefreshToken(AuthPrincipal p, Long sessionId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setIssuer(properties.getIssuer())
                .setSubject(String.valueOf(p.userId()))
                .setId(UUID.randomUUID().toString())           // jti để quản lý phiên
                .claim("typ", "refresh")
                .claim("sid", sessionId)
                .claim("pv", p.passwordVersion())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(properties.getRefreshTtl())))
                .signWith(refreshKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateResetToken(String identifier, String requestId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setSubject(identifier)
                .issuer(properties.getIssuer())   // Email hoặc số điện thoại
                .claim("typ", "reset")                  // Phân biệt với accessToken/refreshToken
                .claim("rid", requestId)                // Ràng buộc requestId
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(properties.getResetTtl()))) // 10 phút
                .signWith(resetKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Ném exception nếu access token không hợp lệ
    public Claims safeParseAccessClaims(String token) {
        try {
            return Jwts.parser()
                    .requireIssuer(properties.getIssuer())
                    .clockSkewSeconds(properties.getClockSkew().toSeconds())
                    .verifyWith((SecretKey) accessKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (ExpiredJwtException e) {
            log.info("Access token expired: {}", e.getMessage());
            throw new JwtAuthenticationException(MessageKeys.SESSION_EXPIRED);
        } catch (SignatureException | SecurityException e) {
            log.warn("Access token signature invalid: {}", e.getMessage());
            throw new JwtAuthenticationException(MessageKeys.SESSION_INVALID);
        }
        catch (MalformedJwtException | IllegalArgumentException e) {
            log.warn("Access token malformed");
            throw new JwtAuthenticationException(MessageKeys.SESSION_INVALID);
        }
    }

    // Ném exception nếu refresh token không hợp lệ
    public Claims parseRefreshClaims(String token) {
        return Jwts.parser()
                .requireIssuer(properties.getIssuer())
                .clockSkewSeconds(properties.getClockSkew().toSeconds())
                .verifyWith((SecretKey) refreshKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // JwtTokenUtils
    public Claims safeParseResetClaims(String token) {
        return Jwts.parser()
                .requireIssuer(properties.getIssuer())
                .clockSkewSeconds(properties.getClockSkew().toSeconds())
                .verifyWith((SecretKey) resetKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
