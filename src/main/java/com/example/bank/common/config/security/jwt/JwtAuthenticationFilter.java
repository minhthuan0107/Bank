package com.example.bank.common.config.security.jwt;



import com.example.bank.common.config.security.UserDetailsServiceImpl;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.auth.JwtAuthenticationException;
import com.example.bank.common.exception.auth.UnauthorizedException;
import com.example.bank.entity.user.User;
import com.example.bank.enums.user.AccountStatus;
import com.example.bank.repository.user.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/*
    Lọc này thực hiện xác thực JWT cho các request đến
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenUtils jwtTokenUtils;
    private final UserDetailsServiceImpl  userDetailsService;
    private final UserRepository userRepository;
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${api.prefix}")
    private String apiPrefix;

    /**
     * Chỉ bypass JWT filter cho API hạ tầng / kỹ thuật
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // API hạ tầng kỹ thuật không cần JWT
        // Webhook từ bên ngoài (Bunny, Stripe, etc.)
        if (uri.startsWith(apiPrefix + "/webhook/")) return true;
        return false;
    }
    // Xác thực JWT cho các request còn lại
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = parseJwt(request);
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Parse + validate access token (signature, issuer, exp, v.v…)
            Claims claims = jwtTokenUtils.safeParseAccessClaims(token);
            // Kiểm tra typ = access
            String typ = (String) claims.get("typ");
            if (!"access".equals(typ)) {
                log.warn("ACCESS-FAIL: typ != access, uri={}", request.getRequestURI());
                    throw new JwtAuthenticationException(MessageKeys.SESSION_INVALID);
            }
            // Lấy userId + passwordVersion từ claims
            Long userId = Long.valueOf(claims.getSubject());
            Integer tokenPv = claims.get("pv", Integer.class);

            // Load user từ database (để kiểm tra status + passwordVersion)
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.warn("ACCESS-FAIL: user not found id={}", userId);
                        return new JwtAuthenticationException(MessageKeys.SESSION_INVALID);
                    });

            // Kiểm tra user locked/disabled/deleted
            ensureUserAllowedToLogin(user);

            // Kiểm tra passwordVersion (nếu user đổi mật khẩu → invalidate access token)
            if (!tokenPv.equals(user.getPasswordVersion())) {
                log.warn("ACCESS-FAIL: password version mismatch id={}, tokenPv={}, userPv={}",
                        userId, tokenPv, user.getPasswordVersion());
                throw new JwtAuthenticationException(MessageKeys.SESSION_INVALID);
            }

            // Load UserDetails chuẩn để gắn vào SecurityContext
            UserDetails userDetails = userDetailsService.loadUserById(userId);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            // Gắn vào SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("ACCESS-SUCCESS: userId={}, uri={}", userId, request.getRequestURI());
            filterChain.doFilter(request, response);

        } catch (JwtAuthenticationException ex) {
            // Đẩy exception ra cho JwtExceptionFilter xử lý
            throw ex;
        }
    }

    // Lấy JWT từ header Authorization
    private String parseJwt(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    //Check trạng thái user trước khi login bằng OAuth
    private void ensureUserAllowedToLogin(User user) {
        if (user.getStatus() == AccountStatus.DELETED) {
            log.warn("AUTH-BLOCKED: userId={} deletedAt={}",
                    user.getId(),
                    user.getDeletedAt());
            throw new UnauthorizedException(
                    MessageKeys.USER_ACCOUNT_DELETED,
                    HttpStatus.UNAUTHORIZED);
        }
        // Check locked
        if (user.getStatus() == AccountStatus.LOCKED) {
            log.warn("AUTH-BLOCKED: userId={} status=LOCKED", user.getId());
            throw new UnauthorizedException(
                    MessageKeys.USER_ACCOUNT_LOCKED,
                    HttpStatus.UNAUTHORIZED);
        }
    }
}
