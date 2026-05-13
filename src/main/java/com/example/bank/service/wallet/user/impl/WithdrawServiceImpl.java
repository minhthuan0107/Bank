package com.example.bank.service.wallet.user.impl;

import com.example.bank.common.config.properties.OtpProperties;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.constants.RedisKeys;
import com.example.bank.common.context.AuthContext;
import com.example.bank.common.exception.auth.OtpException;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.wallet.user.ConfirmWithdrawOtpRequest;
import com.example.bank.dto.request.wallet.user.CreateWithdrawOrderRequest;
import com.example.bank.dto.response.wallet.user.*;
import com.example.bank.entity.wallet.Wallet;
import com.example.bank.entity.wallet.WalletCurrencySettings;
import com.example.bank.entity.wallet.WithdrawOrder;
import com.example.bank.enums.wallet.WithdrawOrderStatus;
import com.example.bank.event.WithdrawOrderPendingAdminEvent;
import com.example.bank.repository.projection.WithdrawDashboardProjection;
import com.example.bank.repository.projection.WithdrawSummaryProjection;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.repository.wallet.WalletCurrencySettingsRepository;
import com.example.bank.repository.wallet.WalletRepository;
import com.example.bank.repository.wallet.WithdrawOrderRepository;
import com.example.bank.service.mail.MailService;
import com.example.bank.service.wallet.user.WithdrawService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final WalletRepository walletRepository;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private final WalletCurrencySettingsRepository walletCurrencySettingsRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public CreateWithdrawOrderResponse createWithdrawOrder(
            CreateWithdrawOrderRequest request,
            Long userId,
            HttpServletRequest httpServletRequest
    ) {

        AuthContext ctx = AuthContext.from(httpServletRequest);

        // ===== RATE LIMIT USER =====
        String rlUserKey = RedisKeys.withdrawRateLimitUser(userId);
        redis.opsForValue().setIfAbsent(rlUserKey, "0", 60, TimeUnit.SECONDS); // atomic set + ttl
        Long userCount = redis.opsForValue().increment(rlUserKey);
        if (userCount != null && userCount > 5) {
            throw new WalletException(
                    MessageKeys.TOO_MANY_REQUESTS,
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }

       // ===== RATE LIMIT IP =====
        String rlIpKey = RedisKeys.withdrawRateLimitIp(ctx.getIp());
        redis.opsForValue().setIfAbsent(rlIpKey, "0", 60, TimeUnit.SECONDS);
        Long ipCount = redis.opsForValue().increment(rlIpKey);
        if (ipCount != null && ipCount > 20) {
            throw new WalletException(
                    MessageKeys.TOO_MANY_REQUESTS,
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }

        // ===== LOCK =====
        String lockKey = RedisKeys.withdrawLockKey(userId);
        Boolean locked = redis.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(locked)) {
            throw new WalletException(
                    MessageKeys.WITHDRAW_TOO_MANY_REQUESTS,
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }
       withdrawRepository.findByUserIdAndStatus(userId, WithdrawOrderStatus.PENDING_OTP)
                .ifPresent(oldOrder -> {
                    String oldOtpKey = RedisKeys.withdrawOtpValueKey(oldOrder.getOrderNo());
                    String otp = redis.opsForValue().get(oldOtpKey);
                    if (otp != null) {
                        // OTP còn hạn → KHÔNG throw
                        throw new WalletException(
                                MessageKeys.WITHDRAW_OTP_STILL_VALID,
                                HttpStatus.CONFLICT, // hoặc custom code
                                oldOrder.getOrderNo() // trả lại orderNo
                        );
                    }

                    // OTP hết hạn → expire order cũ
                    oldOrder.markExpired();
                    withdrawRepository.save(oldOrder);
                });

        try {
            // ===== CHECK MIN WITHDRAW AMOUNT =====
            WalletCurrencySettings settings = walletCurrencySettingsRepository
                    .findByCurrencyAndStatus(request.getCurrency(), "ACTIVE")
                    .orElseThrow(() -> new WalletException(
                            MessageKeys.WALLET_CURRENCY_SETTINGS_NOT_FOUND,
                            HttpStatus.NOT_FOUND
                    ));

            if (request.getAmount().compareTo(settings.getMinWithdrawAmount()) < 0) {
                throw new WalletException(
                        MessageKeys.WITHDRAW_AMOUNT_BELOW_MIN,
                        HttpStatus.BAD_REQUEST
                );
            }
            // ===== CHECK BALANCE =====
            Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                    .orElseThrow(() -> new WalletException(
                            MessageKeys.WALLET_NOT_FOUND,
                            HttpStatus.NOT_FOUND
                    ));
            if (request.getAmount().compareTo(wallet.getAvailableBalance()) > 0) {
                throw new WalletException(
                        MessageKeys.WALLET_INSUFFICIENT_BALANCE,
                        HttpStatus.BAD_REQUEST
                );
            }

            // ===== CREATE ORDER =====
            String orderNo = generateOrderNo();
            WithdrawOrder order = WithdrawOrder.create(
                    userId,
                    orderNo,
                    request.getCurrency(),
                    request.getNetwork(),
                    request.getToAddress(),
                    request.getAmount()
            );

            try {
                withdrawRepository.save(order);
            } catch (DataIntegrityViolationException ex) {
                log.warn("WITHDRAW DUPLICATE userId={}", userId);
                throw new WalletException(
                        MessageKeys.WITHDRAW_ORDER_ALREADY_PENDING,
                        HttpStatus.CONFLICT
                );
            }

            // ===== LẤY EMAIL =====
            String email = userRepository.findEmailByUserId(userId)
                    .orElseThrow(() -> new WalletException(
                            MessageKeys.USER_NOT_FOUND,
                            HttpStatus.NOT_FOUND
                    ));

            // ===== OTP =====
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

        } finally {
            redis.delete(lockKey);
        }
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

        // ===== TRỪ TIỀN KHI OTP ĐÚNG =====
                Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.WALLET_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));
        if (order.getAmount().compareTo(wallet.getAvailableBalance()) > 0) {
            order.markExpired();
            throw new WalletException(
                    MessageKeys.WALLET_INSUFFICIENT_BALANCE,
                    HttpStatus.BAD_REQUEST
            );
        }

        wallet.setAvailableBalance(
                wallet.getAvailableBalance().subtract(order.getAmount())
        );
        wallet.setTotalBalance(
                wallet.getTotalBalance().subtract(order.getAmount())
        );
        walletRepository.save(wallet);

        order.markPendingAdmin();

        applicationEventPublisher.publishEvent(new WithdrawOrderPendingAdminEvent(
                order.getId(),
                userId,
                order.getOrderNo(),
                order.getCurrency(),
                order.getNetwork(),
                order.getToAddress(),
                order.getAmount()
        ));
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
        String cdKey = RedisKeys.withdrawOtpCooldownKey(orderNo);
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

        // Ghi đè giá trị otp mới
        redis.opsForValue().set(
                otpKey,
                otp,
                otpProperties.getTtlSeconds(),
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


    @Override
    public WithdrawOrderPageResponse getUserWithdrawOrders(
            Long userId,
            int page
    ) {
        Pageable pageable = PageRequest.of(
                page,
                DEFAULT_PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "id")
        );

        Page<WithdrawOrder> orders =
                withdrawRepository.findByUserId(userId, pageable);

        if (orders.isEmpty()) {
            return WithdrawOrderPageResponse.builder()
                    .items(List.of())
                    .page(page)
                    .size(pageable.getPageSize())
                    .totalSize(0)
                    .hasNext(false)
                    .build();
        }

        List<WithdrawOrderListResponse> items =
                orders.getContent()
                        .stream()
                        .map(WithdrawOrderListResponse::from)
                        .toList();

        return WithdrawOrderPageResponse.builder()
                .items(items)
                .page(page)
                .size(DEFAULT_PAGE_SIZE)
                .totalSize(orders.getTotalElements())
                .hasNext(orders.hasNext())
                .build();
    }

    @Override
    public WithdrawOrderPageResponse getWithdrawOrders(
            Long userId,
            String orderNo,
            String address,
            WithdrawOrderStatus status,
            Instant fromTime,
            Instant toTime,
            int page
    ) {
        // normalize
        orderNo = normalize(orderNo);
        address = normalize(address);

        // validate time
        if (fromTime != null && toTime != null && fromTime.isAfter(toTime)) {
            throw new WalletException(
                    MessageKeys.INVALID_TIME_RANGE,
                    HttpStatus.BAD_REQUEST
            );
        }

        page = Math.max(page, 0);
        Pageable pageable = PageRequest.of(
                page,
                DEFAULT_PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<WithdrawOrder> result = withdrawRepository.search(
                userId,
                orderNo,
                address,
                status,
                fromTime,
                toTime,
                pageable
        );

        List<WithdrawOrderListResponse> items = result.getContent()
                .stream()
                .map(WithdrawOrderListResponse::from)
                .toList();

        return WithdrawOrderPageResponse.builder()
                .items(items)
                .page(page)
                .size(DEFAULT_PAGE_SIZE)
                .totalSize(result.getTotalElements())
                .hasNext(result.hasNext())
                .build();
    }

    private String normalize(String val) {
        return (val == null || val.isBlank()) ? null : val.trim();
    }

    @Override
    public WithdrawDashboardResponse getWithdrawDashboard(Long userId) {
        WithdrawDashboardProjection p = withdrawRepository.getDashboard(userId);
        return WithdrawDashboardResponse.builder()
                .totalAmount(
                        p != null && p.getTotalAmount() != null
                                ? p.getTotalAmount()
                                : BigDecimal.ZERO
                )
                .successCount(
                        p != null && p.getSuccessCount() != null
                                ? p.getSuccessCount()
                                : 0L
                )
                .failedCount(
                        p != null && p.getFailedCount() != null
                                ? p.getFailedCount()
                                : 0L
                )
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WithdrawSummaryResponse getWithdrawSummary(Long userId) {
        WithdrawSummaryProjection summary = walletRepository.findWithdrawSummaryByUserId(userId)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.WALLET_WITHDRAW_SUMMARY_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        return WithdrawSummaryResponse.builder()
                .walletId(summary.getWalletId())
                .balance(safe(summary.getBalance()))
                .minimumWithdrawalAmount(safe(summary.getMinimumWithdrawalAmount()))
                .currency(summary.getCurrency())
                .build();
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
