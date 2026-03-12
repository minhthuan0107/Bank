package com.example.bank.common.exception.handler;


import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.auth.*;
import com.example.bank.common.exception.base.BusinessException;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler
 *
 * Lớp xử lý ngoại lệ toàn cục cho toàn bộ ứng dụng.
 *
 * Mục tiêu:
 * - Chuẩn hoá response lỗi trả về cho client
 * - Tách rõ các nhóm lỗi: validation, business, auth, domain
 * - Áp dụng i18n (đa ngôn ngữ) cho message
 * - Không để lộ message kỹ thuật (stacktrace, Jackson error, v.v.)
 *
 * Nguyên tắc thiết kế:
 * - Lỗi parse / format → 400 Bad Request
 * - Lỗi validate @Valid → 422 Unprocessable Entity
 * - Lỗi nghiệp vụ → theo HttpStatus được định nghĩa trong Exception
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {
    private final LocalizationUtils i18n;
    private final HttpServletRequest request;

    /**
     * Bắt lỗi validate DTO thông qua @Valid (Bean Validation).
     *
     * Ví dụ:
     * - @NotNull
     * - @NotBlank
     * - @Size
     *
     * Đặc điểm:
     * - JSON đã parse thành công
     * - Nhưng dữ liệu vi phạm rule nghiệp vụ
     *
     * HTTP Status:
     * - 422 Unprocessable Entity
     *
     * Response trả về:
     * - field: tên field bị lỗi
     * - message: message i18n tương ứng với MessageKey
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException ex) {

        String messageKey = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(e -> e.getDefaultMessage())
                .orElse(MessageKeys.REQUEST_INVALID);

        String field = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(e -> e.getField())
                .orElse("unknown");

        log.info("Request-INVALID: path={}, field={}, messageKey={}",
                request.getRequestURI(),
                field,
                messageKey
        );

        String message;
        try {
            message = i18n.getLocalizedMessage(messageKey);
        } catch (NoSuchMessageException e) {
            message = messageKey;
        }

        return ResponseEntity.unprocessableEntity().body(Map.of(
                "status", 422,
                "field", field,
                "message", message
        ));
    }

    /**
     * Bắt lỗi JSON parse / deserialize thất bại.
     *
     * Trường hợp phổ biến:
     * - Enum sai giá trị (VD: "DALY-life")
     * - JSON format không hợp lệ
     *
     * LƯU Ý:
     * - Enum sai xảy ra TRƯỚC @Valid
     * - Không thể dùng Bean Validation để bắt
     *
     * HTTP Status:
     * - 400 Bad Request
     *
     * Trả về message chung, không lộ chi tiết enum hoặc cấu trúc backend.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleInvalidEnum(
            HttpMessageNotReadableException ex
    ) {
        Throwable root = ex.getRootCause();

        // Jackson parse enum sai → InvalidFormatException
        if (root instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException) {
            log.warn("INVALID_ENUM_VALUE {}", root.getMessage());

            return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                            HttpStatus.BAD_REQUEST.value(),
                            i18n.getLocalizedMessage(
                                    MessageKeys.VALIDATION_INVALID_ENUM
                            )
                    )
            );
        }

        // fallback cho các lỗi JSON khác
        return ResponseEntity.badRequest().body(
                ApiResponse.error(
                        HttpStatus.BAD_REQUEST.value(),
                        i18n.getLocalizedMessage(
                                MessageKeys.VALIDATION_INVALID_ENUM
                        )
                )
        );
    }


    /**
     * Bắt các lỗi nghiệp vụ chung (BusinessException).
     *
     * Ví dụ:
     * - Vi phạm rule nghiệp vụ
     * - Thao tác không hợp lệ theo logic domain
     *
     * HTTP Status:
     * - 400 Bad Request
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusiness(BusinessException ex) {
        String localized = i18n.getLocalizedMessage(ex.getMessageKey());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", 400,
                        "type", "BUSINESS",
                        "message", localized
                ));
    }

    /**
     * Bắt lỗi xác thực (Unauthorized).
     *
     * Ví dụ:
     * - Sai token
     * - Hết hạn session
     *
     * HTTP Status:
     * - 401 Unauthorized
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<?> handle401(UnauthorizedException ex) {
        String localized = i18n.getLocalizedMessage(ex.getMessageKey());
        return ResponseEntity.status(
                ex.getStatus()).body(Map.of(
                "status", ex.getStatus().value(),
                "type", ex.getStatus().name(),
                "message", localized
        ));
    }


    /**
     * Bắt lỗi liên quan đến OTP.
     *
     * Có thể kèm retryAfter để client biết thời gian chờ tiếp theo.
     */
    @ExceptionHandler(OtpException.class)
    public ResponseEntity<?> handleOtp(OtpException ex) {
        String message = (ex.getRetryAfter() == null)
                ? i18n.getLocalizedMessage(ex.getMessage())
                : i18n.getLocalizedMessage(ex.getMessage(), ex.getRetryAfter());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 400);
        body.put("message", message);

        if (ex.getRetryAfter() != null) {
            body.put("retryAfter", ex.getRetryAfter());
        }
        return ResponseEntity.status(400).body(body);
    }


    /**
     * Bắt lỗi xác thực JWT.
     */
    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<?> handleJwtAuth(JwtAuthenticationException ex) {
        String localized = i18n.getLocalizedMessage(ex.getMessageKey());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "status", 401,
                        "type", "JWT_AUTH",
                        "message", localized
                ));
    }


    /**
     * Bắt lỗi đăng nhập bị cấm (Forbidden).
     */
    @ExceptionHandler(ForbiddenLoginException.class)
    public ResponseEntity<?> handleForbiddenLogin(ForbiddenLoginException ex) {
        String localized = i18n.getLocalizedMessage(ex.getMessageKey());
        return ResponseEntity.status(403).body(Map.of(
                "status", 403,
                "type", "FORBIDDEN",
                "message",localized
        ));
    }
    /**
     * Bắt lỗi domain liên quan đến Auth.
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<?> handleAuthException(AuthException ex) {
        String localized = i18n.getLocalizedMessage(ex.getMessageKey());
        return ResponseEntity.status(
                ex.getStatus()).body(Map.of(
                "status", ex.getStatus().value(),
                "type", ex.getStatus().name(),
                "message", localized
        ));
    }

    /**
     * Bắt lỗi domain liên quan đến Post.
     */
    @ExceptionHandler(WalletException.class)
    public ResponseEntity<?> handleWalletException(WalletException ex) {
        String localized = i18n.getLocalizedMessage(ex.getMessageKey());
        return ResponseEntity.status(
                ex.getStatus()).body(Map.of(
                "status", ex.getStatus().value(),
                "type", ex.getStatus().name(),
                "message", localized
        ));
    }


}
