package com.example.bank.service.wallet.user.impl;

import com.example.bank.common.config.properties.OtpProperties;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.constants.RedisKeys;
import com.example.bank.common.exception.auth.OtpException;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.wallet.user.ConfirmWithdrawOtpRequest;
import com.example.bank.dto.request.wallet.user.CreateWithdrawOrderRequest;
import com.example.bank.dto.response.wallet.user.CreateWithdrawOrderResponse;
import com.example.bank.entity.wallet.WithdrawOrder;
import com.example.bank.enums.wallet.WithdrawOrderStatus;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.repository.wallet.WithdrawOrderRepository;
import com.example.bank.service.mail.MailService;
import com.example.bank.service.wallet.user.WithdrawService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawServiceImpl implements WithdrawService {

    private final WithdrawOrderRepository withdrawRepository;
    private final StringRedisTemplate redis;
    private final MailService mailService;
    private final UserRepository userRepository;
    private final OtpProperties otpProperties;

    @Override
    @Transactional
    public CreateWithdrawOrderResponse createWithdrawOrder(
            CreateWithdrawOrderRequest request,
            Long userId
    ) {

        // ===== check active withdraw =====
        boolean existsActive =
                withdrawRepository.existsByUserIdAndStatusIn(
                        userId,
                        List.of(
                                WithdrawOrderStatus.PENDING_OTP,
                                WithdrawOrderStatus.PENDING_ADMIN,
                                WithdrawOrderStatus.PROCESSING
                        )
                );

        if (existsActive) {
            throw new WalletException(
                    MessageKeys.WITHDRAW_ORDER_ALREADY_PENDING,
                    HttpStatus.CONFLICT
            );
        }

        // ===== daily limit =====
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        Instant start = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = start.plus(1, ChronoUnit.DAYS);

        boolean exists =
                withdrawRepository.existsByUserIdAndStatusAndCreatedAtBetween(
                        userId,
                        WithdrawOrderStatus.SUCCESS,
                        start,
                        end
                );

        if (exists) {
            throw new WalletException(
                    MessageKeys.WITHDRAW_DAILY_LIMIT_REACHED,
                    HttpStatus.CONFLICT
            );
        }

        // ===== get email =====
        String email = userRepository.findEmailByUserId(userId)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.USER_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        // ===== hourly limit =====
        String hourKey = RedisKeys.withdrawOtpEmailHourlyKey(email);

        Long emailCnt = redis.opsForValue().increment(hourKey);

        if (emailCnt != null && emailCnt == 1) {
            redis.expire(hourKey, 3600, TimeUnit.SECONDS);
        }

        if (emailCnt != null && emailCnt > otpProperties.getLimit().getPerHour()) {

            Long remain = redis.getExpire(hourKey, TimeUnit.SECONDS);

            throw new OtpException(
                    MessageKeys.RATE_LIMIT_HOURLY,
                    remain != null ? remain.intValue() : null
            );
        }

        // ===== cooldown atomic =====
        String cdKey = RedisKeys.withdrawOtpCooldownKey(email);

        Boolean locked = redis.opsForValue().setIfAbsent(
                cdKey,
                "1",
                otpProperties.getCooldownSeconds(),
                TimeUnit.SECONDS
        );

        if (Boolean.FALSE.equals(locked)) {

            Long remain = redis.getExpire(cdKey, TimeUnit.SECONDS);

            throw new OtpException(
                    MessageKeys.OTP_COOLDOWN_ACTIVE,
                    remain != null ? remain.intValue() : null
            );
        }

        // ===== create order =====
        String orderNo = generateOrderNo();

        WithdrawOrder order = WithdrawOrder.create(
                userId,
                orderNo,
                request.getCurrency(),
                request.getNetwork(),
                request.getToAddress(),
                request.getAmount()
        );

        withdrawRepository.save(order);

        // ===== generate OTP =====
        String otp = generateOtp();

        redis.opsForValue().set(
                RedisKeys.withdrawOtpValueKey(orderNo),
                otp,
                otpProperties.getTtlSeconds(),
                TimeUnit.SECONDS
        );

        mailService.sendOtp(email, otp);

        return CreateWithdrawOrderResponse.builder()
                .orderNo(orderNo)
                .amount(order.getAmount())
                .status(order.getStatus())
                .build();
    }


    @Override
    @Transactional
    public void confirmWithdrawOtp(
            Long userId,
            ConfirmWithdrawOtpRequest request
    ) {

        WithdrawOrder order =
                withdrawRepository.findByOrderNoAndUserIdForUpdate(
                                request.getOrderNo(),
                                userId
                        )
                        .orElseThrow(() ->
                                new WalletException(
                                        MessageKeys.WITHDRAW_ORDER_NOT_FOUND,
                                        HttpStatus.NOT_FOUND
                                )
                        );

        if (order.getStatus() != WithdrawOrderStatus.PENDING_OTP) {
            throw new WalletException(
                    MessageKeys.WITHDRAW_INVALID_STATUS,
                    HttpStatus.CONFLICT
            );
        }

        String orderNo = order.getOrderNo();

        String otpKey = RedisKeys.withdrawOtpValueKey(orderNo);
        String attemptKey = RedisKeys.withdrawOtpAttemptKey(orderNo);

        String storedOtp = redis.opsForValue().get(otpKey);

        // ===== OTP expired =====
        if (storedOtp == null) {
            order.markExpired();
            throw new WalletException(
                    MessageKeys.WITHDRAW_OTP_EXPIRED,
                    HttpStatus.GONE
            );
        }

        // ===== OTP invalid =====
        if (!storedOtp.equals(request.getOtp())) {
            Long attempts = redis.opsForValue().increment(attemptKey);
            if (attempts != null && attempts == 1) {
                redis.expire(attemptKey, 600, TimeUnit.SECONDS);
            }
            if (attempts != null && attempts > 5) {
                order.markExpired();
                redis.delete(otpKey);
                throw new WalletException(
                        MessageKeys.WITHDRAW_OTP_TOO_MANY_ATTEMPTS,
                        HttpStatus.TOO_MANY_REQUESTS
                );
            }
            throw new WalletException(
                    MessageKeys.WITHDRAW_OTP_INVALID,
                    HttpStatus.BAD_REQUEST
            );
        }

        // ===== success =====
        redis.delete(otpKey);
        redis.delete(attemptKey);

        order.markPendingAdmin();
    }


    @Override
    @Transactional
    public void resendWithdrawOtp(Long userId, String orderNo) {

        WithdrawOrder order =
                withdrawRepository.findByOrderNoAndUserIdForUpdate(orderNo, userId)
                        .orElseThrow(() ->
                                new WalletException(
                                        MessageKeys.WITHDRAW_ORDER_NOT_FOUND,
                                        HttpStatus.NOT_FOUND
                                )
                        );

        if (order.getStatus() != WithdrawOrderStatus.PENDING_OTP) {
            throw new WalletException(
                    MessageKeys.WITHDRAW_INVALID_STATUS,
                    HttpStatus.CONFLICT
            );
        }

        String email = userRepository.findEmailByUserId(userId)
                .orElseThrow(() ->
                        new WalletException(
                                MessageKeys.USER_NOT_FOUND,
                                HttpStatus.NOT_FOUND
                        )
                );

        String otpKey = RedisKeys.withdrawOtpValueKey(orderNo);
        String cdKey = RedisKeys.withdrawOtpCooldownKey(email);
        String hourKey = RedisKeys.withdrawOtpEmailHourlyKey(email);

        // ===== cooldown =====
        Boolean locked = redis.opsForValue().setIfAbsent(
                cdKey,
                "1",
                otpProperties.getCooldownSeconds(),
                TimeUnit.SECONDS
        );
        if (Boolean.FALSE.equals(locked)) {
            Long remain = redis.getExpire(cdKey, TimeUnit.SECONDS);
            throw new OtpException(
                    MessageKeys.OTP_COOLDOWN_ACTIVE,
                    remain != null ? remain.intValue() : null
            );
        }

        // ===== email hourly limit =====
        Long emailCnt = redis.opsForValue().increment(hourKey);
        if (emailCnt != null && emailCnt == 1) {
            redis.expire(hourKey, 3600, TimeUnit.SECONDS);
        }

        if (emailCnt != null && emailCnt > otpProperties.getLimit().getPerHour()) {
            Long remain = redis.getExpire(hourKey, TimeUnit.SECONDS);
            log.warn(
                    "OTP-FAIL reason=RATE_LIMIT scope=EMAIL_HOUR email={} count={}",
                    maskEmail(email),
                    emailCnt
            );

            throw new OtpException(
                    MessageKeys.RATE_LIMIT_HOURLY,
                    remain != null ? remain.intValue() : null
            );
        }

        // ===== generate otp =====
        String otp = generateOtp();

        redis.opsForValue().set(
                otpKey,
                otp,
                otpProperties.getTtlSeconds(),
                TimeUnit.SECONDS
        );

        // ===== set cooldown =====
        redis.opsForValue().set(
                cdKey,
                "1",
                otpProperties.getCooldownSeconds(),
                TimeUnit.SECONDS
        );

        mailService.sendOtp(email, otp);
    }

    private String generateOtp() {
        int otp = ThreadLocalRandom.current().nextInt(100000, 999999);
        return String.valueOf(otp);
    }



    private String generateOrderNo() {
        return "WD" +
                System.currentTimeMillis() +
                ThreadLocalRandom.current().nextInt(100, 999);
    }

    private String maskEmail(String email) {
        int at = email.indexOf("@");
        if (at <= 2) return email;
        return email.substring(0, 2)
                + "***"
                + email.substring(at);
    }
}
