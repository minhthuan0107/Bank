package com.example.bank.service.otp.impl;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.constants.RedisKeys;
import com.example.bank.common.exception.auth.AuthException;
import com.example.bank.dto.request.otp.OtpRequest;
import com.example.bank.dto.request.wallet.user.ForgotPasswordOtpRequest;
import com.example.bank.dto.request.wallet.user.ResetPasswordRequest;
import com.example.bank.entity.user.User;
import com.example.bank.enums.otp.OtpPurpose;
import com.example.bank.enums.user.AccountStatus;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.service.otp.ForgotPasswordService;
import com.example.bank.service.otp.OtpService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForgotPasswordServiceImpl implements ForgotPasswordService {

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final StringRedisTemplate redis;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public void requestResetOtp(ForgotPasswordOtpRequest request, HttpServletRequest httpRequest) {
        String username = normalize(request.getUsername());

        Optional<User> userOpt = userRepository.findByUsername(username);

        /*
         * Không throw USER_NOT_FOUND ở forgot password.
         * Mục đích: tránh bị tool dò username nào tồn tại trong hệ thống.
         * Controller vẫn trả message generic:
         * "Nếu tài khoản tồn tại, OTP đã được gửi..."
         */
        if (userOpt.isEmpty()) {
            log.warn("FORGOT-PASSWORD-OTP-SKIP reason=USER_NOT_FOUND username={}", username);
            return;
        }

        User user = userOpt.get();

        /*
         * Nếu tài khoản không active cũng không báo rõ ra ngoài.
         */
        if (user.getStatus() != AccountStatus.ACTIVE) {
            log.warn("FORGOT-PASSWORD-OTP-SKIP reason=USER_NOT_ACTIVE userId={}", user.getId());
            return;
        }

        /*
         * FE không gửi email.
         * Backend tự lấy email đã đăng ký của user rồi gọi OTP service với purpose RESET.
         *
         * Lưu ý:
         * OTP service hiện đang lưu Redis key theo email:
         * email:{email} -> otp key
         * Nên API reset password phía sau cũng phải tìm user theo username,
         * lấy email của user, rồi check OTP theo email đó.
         */
        OtpRequest otpRequest = new OtpRequest();
        otpRequest.setEmail(user.getEmail());
        otpRequest.setPurpose(OtpPurpose.RESET);

        otpService.handleOtpRequest(otpRequest, httpRequest);

        log.info("FORGOT-PASSWORD-OTP-SENT userId={} email={}", user.getId(), maskEmail(user.getEmail()));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
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

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String username = normalize(request.getUsername());
        String otp = request.getOtp() == null ? "" : request.getOtp().trim();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException(
                        MessageKeys.RESET_PASSWORD_INVALID_REQUEST,
                        HttpStatus.BAD_REQUEST
                ));

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new AuthException(
                    MessageKeys.USER_ACCOUNT_NOT_ACTIVE,
                    HttpStatus.FORBIDDEN
            );
        }

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new AuthException(
                    MessageKeys.PASSWORD_CONFIRM_NOT_MATCH,
                    HttpStatus.BAD_REQUEST
            );
        }

        /*
         * OTP service hiện đang lưu Redis key theo email:
         * idKey = "email:" + email
         * nên reset password phải check lại theo email của user trong DB.
         */
        String idKey = "email:" + user.getEmail();
        String otpKey = RedisKeys.otpValueKey(idKey);

        String cachedOtp = redis.opsForValue().get(otpKey);

        if (cachedOtp == null) {
            throw new AuthException(
                    MessageKeys.OTP_EXPIRED_OR_NOT_FOUND,
                    HttpStatus.BAD_REQUEST
            );
        }

        if (!cachedOtp.equals(otp)) {
            throw new AuthException(
                    MessageKeys.OTP_INVALID,
                    HttpStatus.BAD_REQUEST
            );
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordVersion(user.getPasswordVersion()+ 1 );
        userRepository.save(user);

        /*
         * OTP dùng xong phải xoá để tránh dùng lại.
         */
        redis.delete(otpKey);

        log.info("RESET-PASSWORD-SUCCESS userId={}", user.getId());
    }


}