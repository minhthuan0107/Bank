package com.example.bank.common.security.jwt;

import com.example.angelproject.common.exception.auth.JwtAuthenticationException;
import com.example.angelproject.common.exception.auth.UnauthorizedException;
import com.example.angelproject.common.exception.base.HasMessageKey;
import com.example.angelproject.common.utils.LocalizationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
/*
    Lọc này được đặt sau JwtAuthenticationFilter để bắt và xử lý các JwtAuthenticationException
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtExceptionFilter extends OncePerRequestFilter {

    private final LocalizationUtils i18n;
    private final ObjectMapper objectMapper;

    // Bắt và xử lý các JwtAuthenticationException do JwtAuthenticationFilter ném ra
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            filterChain.doFilter(request, response);

        } catch (JwtAuthenticationException ex) {
            handleJwtException(ex, request, response);

        } catch (UnauthorizedException ex) {
            handleJwtException(ex, request, response);
        }
    }

    // Xử lý JwtAuthenticationException và UnauthorizedException
    private void handleJwtException(
            HasMessageKey ex,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        // Nếu response đã được commit (gửi về client) thì không làm gì cả
        if (response.isCommitted()) return;

        String messageKey = ex.getMessageKey();
        String localized = i18n.getLocalizedMessage(messageKey);

        log.info("JWT-ERROR key={} uri={}",
                messageKey,
                request.getRequestURI()
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json; charset=UTF-8");

        // Tạo response body
        Map<String, Object> body = new HashMap<>();
        body.put("status", 401);
        body.put("message", localized);
        body.put("data", null);

        // Ghi response body
        response.getWriter().write(
                objectMapper.writeValueAsString(body)
        );

        // Đảm bảo dữ liệu được gửi về client ngay lập tức
        response.flushBuffer();
    }
}
