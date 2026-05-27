package com.example.bank.common.config.ratelimit;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GlobalRateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String LUA_SCRIPT = """
            -- KEYS:
            -- 1 banSecondKey
            -- 2 banMinuteKey
            -- 3 banHourKey
            -- 4 secondCounterKey
            -- 5 minuteCounterKey
            -- 6 hourCounterKey
            
            -- ARGV:
            -- 1 secondLimit
            -- 2 minuteLimit
            -- 3 hourLimit
            -- 4 secondWindowTtlSeconds
            -- 5 minuteWindowTtlSeconds
            -- 6 hourWindowTtlSeconds
            -- 7 secondBanTtlSeconds
            -- 8 minuteBanTtlSeconds
            -- 9 hourBanTtlSeconds
            
            local banSecond = redis.call('TTL', KEYS[1])
            if banSecond > 0 then
                return {'BLOCKED', 'SECOND', tostring(banSecond)}
            end
            
            local banMinute = redis.call('TTL', KEYS[2])
            if banMinute > 0 then
                return {'BLOCKED', 'MINUTE', tostring(banMinute)}
            end
            
            local banHour = redis.call('TTL', KEYS[3])
            if banHour > 0 then
                return {'BLOCKED', 'HOUR', tostring(banHour)}
            end
            
            local secondCount = redis.call('INCR', KEYS[4])
            if secondCount == 1 then
                redis.call('EXPIRE', KEYS[4], ARGV[4])
            end
            
            if secondCount > tonumber(ARGV[1]) then
                redis.call('SET', KEYS[1], '1', 'EX', ARGV[7])
                return {'LIMITED', 'SECOND', ARGV[7]}
            end
            
            local minuteCount = redis.call('INCR', KEYS[5])
            if minuteCount == 1 then
                redis.call('EXPIRE', KEYS[5], ARGV[5])
            end
            
            if minuteCount > tonumber(ARGV[2]) then
                redis.call('SET', KEYS[2], '1', 'EX', ARGV[8])
                return {'LIMITED', 'MINUTE', ARGV[8]}
            end
            
            local hourCount = redis.call('INCR', KEYS[6])
            if hourCount == 1 then
                redis.call('EXPIRE', KEYS[6], ARGV[6])
            end
            
            if hourCount > tonumber(ARGV[3]) then
                redis.call('SET', KEYS[3], '1', 'EX', ARGV[9])
                return {'LIMITED', 'HOUR', ARGV[9]}
            end
            
            return {'OK', 'NONE', '0'}
            """;

    private final DefaultRedisScript<List> rateLimitScript =
            new DefaultRedisScript<>(LUA_SCRIPT, List.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        if (!path.startsWith("/api/")) {
            return true;
        }

        return shouldSkip(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String identity = resolveIdentity(request);


        String baseKey = "rate-limit:" + identity;

        String banSecondKey = baseKey + ":ban:second";
        String banMinuteKey = baseKey + ":ban:minute";
        String banHourKey = baseKey + ":ban:hour";

        String secondCounterKey = baseKey + ":counter:second";
        String minuteCounterKey = baseKey + ":counter:minute";
        String hourCounterKey = baseKey + ":counter:hour";

        try {
            List<?> result = redisTemplate.execute(
                    rateLimitScript,
                    List.of(
                            banSecondKey,
                            banMinuteKey,
                            banHourKey,
                            secondCounterKey,
                            minuteCounterKey,
                            hourCounterKey
                    ),

                    /*
                     * Production global API rate limit:
                     * - 30 request / 1 giây
                     * - 300 request / 1 phút
                     * - 3000 request / 1 giờ
                     *
                     * Lưu ý:
                     * - Đây là global limit cho toàn bộ API.
                     * - Không nên quá gắt vì Angular dashboard có thể gọi nhiều API cùng lúc.
                     * - Login / OTP / Admin nên có limit riêng chặt hơn.
                     */
                    "30",
                    "300",
                    "3000",

                    String.valueOf(Duration.ofSeconds(1).toSeconds()),
                    String.valueOf(Duration.ofMinutes(1).toSeconds()),
                    String.valueOf(Duration.ofHours(1).toSeconds()),

                    /*
                     * Ban time:
                     * - vượt limit giây  -> ban 1 phút
                     * - vượt limit phút  -> ban 10 phút
                     * - vượt limit giờ   -> ban 1 tiếng
                     */
                    String.valueOf(Duration.ofMinutes(1).toSeconds()),
                    String.valueOf(Duration.ofMinutes(10).toSeconds()),
                    String.valueOf(Duration.ofHours(1).toSeconds())
            );
            log.debug("RATE_LIMIT_RESULT identity={} path={} result={}", identity, path, result);

            if (result == null || result.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }

            String status = String.valueOf(result.get(0));
            String scope = String.valueOf(result.get(1));
            long retryAfterSeconds = Long.parseLong(String.valueOf(result.get(2)));

            if ("OK".equals(status)) {
                filterChain.doFilter(request, response);
                return;
            }

            writeTooManyRequests(response, scope, retryAfterSeconds);

        } catch (Exception e) {
            /*
             * Fail-open:
             * Redis lỗi thì cho request đi tiếp để app không chết.
             */
            log.warn(
                    "Rate limit Redis error identity={} path={} error={}",
                    identity,
                    path,
                    e.getMessage()
            );

            filterChain.doFilter(request, response);
        }
    }

    private String resolveIdentity(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return "user:" + userDetails.getId();
        }

        return "ip:" + getClientIp(request);
    }

    private String getClientIp(HttpServletRequest request) {
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");

        if (cfConnectingIp != null && !cfConnectingIp.isBlank()) {
            return cfConnectingIp.trim();
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");

        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private boolean shouldSkip(String path) {
        return path.startsWith("/api/v1/auth/refresh-token")
                || path.startsWith("/api/v1/auth/logout")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator/health");
    }

    private void writeTooManyRequests(
            HttpServletResponse response,
            String scope,
            long retryAfterSeconds
    ) throws IOException {

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("message", buildMessage(scope));
        body.put("error", "TOO_MANY_REQUESTS");
        body.put("scope", scope);
        body.put("retry_after_seconds", retryAfterSeconds);

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private String buildMessage(String scope) {
        return switch (scope) {
            case "SECOND" -> "Too many requests per second. Please try again later.";
            case "MINUTE" -> "Too many requests per minute. Please try again later.";
            case "HOUR" -> "Too many requests per hour. Please try again later.";
            default -> "Too many requests. Please try again later.";
        };
    }
}