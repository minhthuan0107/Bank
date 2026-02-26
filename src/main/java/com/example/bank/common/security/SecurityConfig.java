package com.example.bank.common.security;

import com.example.angelproject.common.config.constants.MessageKeys;
import com.example.angelproject.common.config.security.jwt.JwtAuthenticationFilter;
import com.example.angelproject.common.config.security.jwt.JwtExceptionFilter;
import com.example.angelproject.common.config.security.jwt.JwtProperties;
import com.example.angelproject.common.utils.LocalizationUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(jsr250Enabled = true)
@EnableConfigurationProperties(JwtProperties.class)
/*
    Cấu hình bảo mật Spring Security
 */
public class SecurityConfig {
    private final UserDetailsService userDetailsService; // chính là UserDetailsServiceImpl @Service
    private final CorsConfigurationSource corsConfigurationSource;

    @Value("${api.prefix}")
    private String apiPrefix;

    private final LocalizationUtils i18n;

    // Mã hóa password với BCrypt
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // Cấu hình AuthenticationProvider dùng DaoAuthenticationProvider với UserDetailsService và PasswordEncoder
    @Bean
    public AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    // Cấu hình AuthenticationManager
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    // Cấu hình SecurityFilterChain
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter, JwtExceptionFilter jwtExceptionFilter) throws Exception {

        http
                // REST API không dùng CSRF
                .csrf(AbstractHttpConfigurer::disable)

                // bật CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // stateless session
                .sessionManagement(sess ->
                        sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // tắt form login & http basic
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // cấu hình quyền truy cập
                .authorizeHttpRequests(auth -> auth
                                // Webhook từ hệ thống ngoài
                                .requestMatchers(HttpMethod.POST,
                                        apiPrefix + "/webhook/bunny/video-uploaded"
                                ).permitAll()

                                // Public static / docs
                                .requestMatchers(
                                        apiPrefix + "/public/**",
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**"
                                ).permitAll()
                        // Để @PreAuthorize xử lý cho các request cần xác thực  , nếu không thì cho phép tất cả
                        .anyRequest().permitAll()
                )
                // Filter bắt lỗi JWT (BỌC NGOÀI)
                .addFilterBefore(jwtExceptionFilter, UsernamePasswordAuthenticationFilter.class)
                // Filter xác thực JWT (BÊN TRONG)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // xử lý lỗi Authentication & Authorization
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("""
                                        {"status":401,"message":"%s"}
                                    """.formatted(
                                    i18n.getLocalizedMessage(MessageKeys.AUTHENTICATION_FAILED)
                            ));
                        })
                        // Authorization failure (đã login nhưng thiếu quyền)
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("""
                                        {"status":403,"message":"%s"}
                                    """.formatted(i18n.getLocalizedMessage(MessageKeys.ACCESS_DENIED)));
                        })
                );
        return http.build();
    }
}
