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
import com.example.bank.entity.wallet.WalletSettings;
import com.example.bank.entity.wallet.WalletExternalAddress;
import com.example.bank.entity.wallet.WithdrawOrder;
import com.example.bank.enums.wallet.CryptoNetwork;
import com.example.bank.enums.wallet.Stablecoin;
import com.example.bank.enums.wallet.WithdrawOrderStatus;
import com.example.bank.event.WithdrawOrderPendingAdminEvent;
import com.example.bank.repository.projection.WithdrawDashboardProjection;
import com.example.bank.repository.projection.WithdrawSummaryProjection;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.repository.wallet.WalletSettingsRepository;
import com.example.bank.repository.wallet.WalletExternalAddressRepository;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
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
    private final WalletSettingsRepository walletSettingsRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final WalletExternalAddressRepository walletExternalAddressRepository;
    private final PlatformTransactionManager transactionManager;

    @Override
    @Transactional
    public CreateWithdrawOrderResponse createWithdrawOrder(
            CreateWithdrawOrderRequest request,
            Long userId,
            HttpServletRequest httpServletRequest
    ) {
        AuthContext ctx = AuthContext.from(httpServletRequest);

        BigDecimal amount = request.getAmount();

        // ===== VALIDATE AMOUNT =====
        if (amount == null || amount.signum() <= 0) {
            throw new WalletException(
                    MessageKeys.WITHDRAW_AMOUNT_BELOW_MIN,
                    HttpStatus.BAD_REQUEST
            );
        }

        // ===== RATE LIMIT =====
        checkWithdrawRateLimit(
                RedisKeys.withdrawRateLimitUser(userId),
                5
        );

        checkWithdrawRateLimit(
                RedisKeys.withdrawRateLimitIp(ctx.getIp()),
                20
        );

        // ===== LOCK USD WALLET =====
        Wallet wallet = walletRepository
                .findByUserIdAndCurrencyForUpdate(
                        userId,
                        Stablecoin.USD
                )
                .orElseThrow(() -> new WalletException(
                        MessageKeys.WALLET_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        // ===== CHECK PENDING ADMIN =====
        if (withdrawRepository.existsByUserIdAndStatus(
                userId,
                WithdrawOrderStatus.PENDING_ADMIN
        )) {
            throw new WalletException(
                    MessageKeys.WITHDRAW_ORDER_PENDING_ADMIN,
                    HttpStatus.CONFLICT
            );
        }

        // ===== CHECK PENDING OTP =====
        Optional<WithdrawOrder> pendingOtpOrder = withdrawRepository
                .findByUserIdAndStatus(
                        userId,
                        WithdrawOrderStatus.PENDING_OTP
                );

        if (pendingOtpOrder.isPresent()) {
            WithdrawOrder oldOrder = pendingOtpOrder.get();

            String oldOtpKey = RedisKeys.withdrawOtpValueKey(
                    oldOrder.getOrderNo()
            );

            Boolean otpExists = redis.hasKey(oldOtpKey);

            // OTP cũ vẫn còn hiệu lực
            if (Boolean.TRUE.equals(otpExists)) {
                // Không tạo order mới.
                // Trả thông tin order cũ để FE tiếp tục nhập OTP.
                return CreateWithdrawOrderResponse.builder()
                        .orderNo(oldOrder.getOrderNo())
                        .amount(oldOrder.getAmount())
                        .status(oldOrder.getStatus())
                        .build();
            }

            // Không xác định được trạng thái Redis: không tạo order mới
            if (otpExists == null) {
                throw new WalletException(
                        MessageKeys.WITHDRAW_OTP_STILL_VALID,
                        HttpStatus.CONFLICT,
                        oldOrder.getOrderNo()
                );
            }
        }

        // ===== CHECK SUCCESSFUL WITHDRAWAL TODAY (UTC) =====
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);

        Instant startOfDay = todayUtc
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        Instant nextDay = todayUtc
                .plusDays(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        boolean successfulToday = withdrawRepository
                .existsByUserIdAndStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        userId,
                        WithdrawOrderStatus.SUCCESS,
                        startOfDay,
                        nextDay
                );

        if (successfulToday) {
            throw new WalletException(
                    MessageKeys.WITHDRAW_DAILY_LIMIT_REACHED,
                    HttpStatus.CONFLICT
            );
        }

        // ===== GET EXTERNAL ADDRESS =====
        WalletExternalAddress externalAddress = walletExternalAddressRepository
                .findByWalletId(wallet.getId())
                .orElseThrow(() -> new WalletException(
                        MessageKeys.WALLET_EXTERNAL_ADDRESS_REQUIRED,
                        HttpStatus.BAD_REQUEST
                ));

        // ===== GET REGISTERED CURRENCY + NETWORK =====
        Stablecoin currency = externalAddress.getCurrency();
        CryptoNetwork network = externalAddress.getNetwork();

        // ===== VALIDATE CURRENCY + NETWORK =====
        boolean validNetwork =
                (currency == Stablecoin.USDT
                        && (network == CryptoNetwork.TRC20
                        || network == CryptoNetwork.BEP20))
                        ||
                        (currency == Stablecoin.USDC
                                && network == CryptoNetwork.BASE);

        if (!validNetwork) {
            throw new WalletException(
                    MessageKeys.WALLET_EXTERNAL_ADDRESS_INVALID_NETWORK,
                    HttpStatus.BAD_REQUEST
            );
        }

        // ===== GET SHARED WALLET SETTINGS =====
        WalletSettings settings = walletSettingsRepository
                .findByIdAndStatus(1L, "ACTIVE")
                .orElseThrow(() -> new WalletException(
                        MessageKeys.WALLET_SETTINGS_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        // ===== CHECK MIN WITHDRAW AMOUNT =====
        if (amount.compareTo(settings.getMinWithdrawAmount()) < 0) {
            throw new WalletException(
                    MessageKeys.WITHDRAW_AMOUNT_BELOW_MIN,
                    HttpStatus.BAD_REQUEST
            );
        }

        // ===== CHECK AVAILABLE BALANCE =====
        if (amount.compareTo(wallet.getAvailableBalance()) > 0) {
            throw new WalletException(
                    MessageKeys.WALLET_INSUFFICIENT_BALANCE,
                    HttpStatus.BAD_REQUEST
            );
        }

        // ===== GET USER EMAIL =====
        String email = userRepository
                .findEmailByUserId(userId)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.USER_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        // ===== EXPIRE OLD OTP ORDER =====
        // Chỉ thực hiện sau khi các validation đã thành công.
        if (pendingOtpOrder.isPresent()) {

            WithdrawOrder oldOrder = pendingOtpOrder.get();

            oldOrder.markExpired();


            withdrawRepository.saveAndFlush(oldOrder);
        }

        // ===== CREATE WITHDRAW ORDER =====
        String orderNo = generateOrderNo();

        WithdrawOrder order = WithdrawOrder.create(
                userId,
                orderNo,
                currency,
                network.name(),
                externalAddress.getAddress(),
                amount
        );
        order.setOtpExpiresAt(
                Instant.now().plusSeconds(otpProperties.getTtlSeconds())
        );
        withdrawRepository.saveAndFlush(order);

        // ===== GENERATE OTP =====
        String otp = generateOtp();
        String otpKey = RedisKeys.withdrawOtpValueKey(orderNo);

        // ===== SEND EMAIL AFTER DB COMMIT =====
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        try {
                            mailService.sendOtp(email, otp);
                        } catch (Exception ex) {
                            log.error(
                                    "WITHDRAW_OTP_EMAIL_FAILED orderNo={} userId={}",
                                    orderNo,
                                    userId,
                                    ex
                            );
                        }
                    }

                    @Override
                    public void afterCompletion(int status) {

                        if (status != TransactionSynchronization.STATUS_COMMITTED) {
                            try {
                                redis.delete(otpKey);
                            } catch (Exception ex) {
                                log.error(
                                        "WITHDRAW_OTP_CLEANUP_FAILED orderNo={}",
                                        orderNo,
                                        ex
                                );
                            }
                        }
                    }
                }
        );

        // ===== STORE OTP =====
        redis.opsForValue().set(
                otpKey,
                otp,
                otpProperties.getTtlSeconds(),
                TimeUnit.SECONDS
        );

        log.info(
                "WITHDRAW_ORDER_CREATED orderNo={} userId={} currency={} network={}",
                orderNo,
                userId,
                currency,
                network
        );

        return CreateWithdrawOrderResponse.builder()
                .orderNo(orderNo)
                .amount(order.getAmount())
                .status(order.getStatus())
                .build();
    }

    private void checkWithdrawRateLimit(String key, long maxRequests) {

        redis.opsForValue().setIfAbsent(
                key,
                "0",
                60,
                TimeUnit.SECONDS
        );

        Long currentCount = redis.opsForValue().increment(key);

        if (currentCount == null || currentCount > maxRequests) {
            throw new WalletException(
                    MessageKeys.TOO_MANY_REQUESTS,
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }
    }

    @Override
    public void confirmWithdrawOtp(
            Long userId,
            ConfirmWithdrawOtpRequest request
    ) {
        ConfirmOtpResult result = newTransaction().execute(tx -> {

            // ===== LOCK USD WALLET =====
            Wallet wallet = walletRepository
                    .findByUserIdAndCurrencyForUpdate(userId, Stablecoin.USD)
                    .orElseThrow(() -> new WalletException(
                            MessageKeys.WALLET_NOT_FOUND,
                            HttpStatus.NOT_FOUND
                    ));

            // ===== LOCK WITHDRAW ORDER =====
            WithdrawOrder order = withdrawRepository
                    .findByOrderNoAndUserIdForUpdate(request.getOrderNo(), userId)
                    .orElseThrow(() -> new WalletException(
                            MessageKeys.WITHDRAW_ORDER_NOT_FOUND,
                            HttpStatus.NOT_FOUND
                    ));

            if (order.getStatus() == WithdrawOrderStatus.EXPIRED) {
                return ConfirmOtpResult.EXPIRED;
            }

            if (order.getStatus() != WithdrawOrderStatus.PENDING_OTP) {
                throw new WalletException(
                        MessageKeys.WITHDRAW_INVALID_STATUS,
                        HttpStatus.CONFLICT
                );
            }

            String orderNo = order.getOrderNo();
            String otpKey = RedisKeys.withdrawOtpValueKey(orderNo);
            String attemptKey = RedisKeys.withdrawOtpAttemptKey(orderNo);

            // ===== CHECK OTP EXPIRATION (UTC) =====
            Instant now = Instant.now();

            if (order.getOtpExpiresAt() == null
                    || !now.isBefore(order.getOtpExpiresAt())) {

                order.markExpired();
                cleanupWithdrawOtpAfterCommit(orderNo);

                return ConfirmOtpResult.EXPIRED;
            }

            // ===== GET OTP FROM REDIS =====
            String storedOtp = redis.opsForValue().get(otpKey);

            if (storedOtp == null) {
                order.markExpired();
                cleanupWithdrawOtpAfterCommit(orderNo);

                return ConfirmOtpResult.EXPIRED;
            }

            // ===== CHECK MAXIMUM ATTEMPTS =====
            String attemptValue = redis.opsForValue().get(attemptKey);

            long usedAttempts = attemptValue == null
                    ? 0L
                    : Long.parseLong(attemptValue);

            if (usedAttempts >= 5) {
                order.markExpired();
                cleanupWithdrawOtpAfterCommit(orderNo);

                return ConfirmOtpResult.TOO_MANY_ATTEMPTS;
            }

            // ===== INVALID OTP =====
            if (!storedOtp.equals(request.getOtp())) {

                Long attempts = redis.opsForValue().increment(attemptKey);

                if (attempts == null) {
                    throw new WalletException(
                            MessageKeys.TOO_MANY_REQUESTS,
                            HttpStatus.SERVICE_UNAVAILABLE
                    );
                }

                if (attempts == 1) {
                    redis.expire(attemptKey, 600, TimeUnit.SECONDS);
                }

                if (attempts >= 5) {
                    order.markExpired();
                    cleanupWithdrawOtpAfterCommit(orderNo);

                    return ConfirmOtpResult.TOO_MANY_ATTEMPTS;
                }

                return ConfirmOtpResult.INVALID;
            }

            // ===== CHECK AVAILABLE BALANCE =====
            BigDecimal amount = order.getAmount();

            if (amount.compareTo(wallet.getAvailableBalance()) > 0) {
                order.markExpired();
                cleanupWithdrawOtpAfterCommit(orderNo);

                return ConfirmOtpResult.INSUFFICIENT_BALANCE;
            }

            // ===== DEDUCT BALANCE ONLY AFTER OTP CONFIRMED =====
            wallet.setAvailableBalance(
                    wallet.getAvailableBalance().subtract(amount)
            );

            wallet.setTotalBalance(
                    wallet.getTotalBalance().subtract(amount)
            );

            // ===== MOVE ORDER TO PENDING_ADMIN =====
            order.markPendingAdmin();

            // ===== DELETE OTP AFTER COMMIT =====
            cleanupWithdrawOtpAfterCommit(orderNo);

            // Listener cần dùng TransactionalEventListener AFTER_COMMIT
            applicationEventPublisher.publishEvent(
                    new WithdrawOrderPendingAdminEvent(
                            order.getId(),
                            userId,
                            order.getOrderNo(),
                            order.getCurrency(),
                            order.getNetwork(),
                            order.getToAddress(),
                            order.getAmount()
                    )
            );

            log.info(
                    "WITHDRAW_OTP_CONFIRMED orderNo={} userId={} amount={}",
                    orderNo,
                    userId,
                    amount
            );

            return ConfirmOtpResult.CONFIRMED;
        });

        if (result == null) {
            throw new IllegalStateException("Withdraw OTP confirmation returned no result");
        }

        // Lúc này transaction đã commit.
        switch (result) {
            case CONFIRMED -> {
                return;
            }
            case INVALID -> throw new WalletException(
                    MessageKeys.WITHDRAW_OTP_INVALID,
                    HttpStatus.BAD_REQUEST
            );
            case EXPIRED -> throw new WalletException(
                    MessageKeys.WITHDRAW_OTP_EXPIRED,
                    HttpStatus.GONE
            );
            case TOO_MANY_ATTEMPTS -> throw new WalletException(
                    MessageKeys.WITHDRAW_OTP_TOO_MANY_ATTEMPTS,
                    HttpStatus.TOO_MANY_REQUESTS
            );
            case INSUFFICIENT_BALANCE -> throw new WalletException(
                    MessageKeys.WALLET_INSUFFICIENT_BALANCE,
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @Override
    public void resendWithdrawOtp(Long userId, String orderNo) {

        ResendOtpResult result = newTransaction().execute(tx -> {

            // ===== LOCK USD WALLET =====
            Wallet wallet = walletRepository
                    .findByUserIdAndCurrencyForUpdate(userId, Stablecoin.USD)
                    .orElseThrow(() -> new WalletException(
                            MessageKeys.WALLET_NOT_FOUND,
                            HttpStatus.NOT_FOUND
                    ));

            // ===== LOCK WITHDRAW ORDER =====
            WithdrawOrder order = withdrawRepository
                    .findByOrderNoAndUserIdForUpdate(orderNo, userId)
                    .orElseThrow(() -> new WalletException(
                            MessageKeys.WITHDRAW_ORDER_NOT_FOUND,
                            HttpStatus.NOT_FOUND
                    ));

            // ===== CHECK STATUS =====
            if (order.getStatus() == WithdrawOrderStatus.EXPIRED) {
                return ResendOtpResult.EXPIRED;
            }

            if (order.getStatus() != WithdrawOrderStatus.PENDING_OTP) {
                throw new WalletException(
                        MessageKeys.WITHDRAW_INVALID_STATUS,
                        HttpStatus.CONFLICT
                );
            }

            // ===== CHECK EXPIRATION =====
            Instant now = Instant.now();

            if (order.getOtpExpiresAt() == null
                    || !now.isBefore(order.getOtpExpiresAt())) {

                order.markExpired();
                cleanupWithdrawOtpAfterCommit(orderNo);

                return ResendOtpResult.EXPIRED;
            }

            // ===== GET EMAIL =====
            String email = userRepository.findEmailByUserId(userId)
                    .orElseThrow(() -> new WalletException(
                            MessageKeys.USER_NOT_FOUND,
                            HttpStatus.NOT_FOUND
                    ));

            String otpKey = RedisKeys.withdrawOtpValueKey(orderNo);
            String cdKey = RedisKeys.withdrawOtpCooldownKey(orderNo);
            String hourKey = RedisKeys.withdrawOtpEmailHourlyKey(email);

            // ===== COOLDOWN =====
            Boolean acquired = redis.opsForValue().setIfAbsent(
                    cdKey,
                    "1",
                    otpProperties.getCooldownSeconds(),
                    TimeUnit.SECONDS
            );

            if (!Boolean.TRUE.equals(acquired)) {
                Long remain = redis.getExpire(cdKey, TimeUnit.SECONDS);

                throw new OtpException(
                        MessageKeys.OTP_COOLDOWN_ACTIVE,
                        remain != null ? remain.intValue() : null
                );
            }

            // ===== EMAIL HOURLY LIMIT =====
            Long emailCount = redis.opsForValue().increment(hourKey);

            if (emailCount == null) {
                throw new IllegalStateException("Unable to update OTP email rate limit");
            }

            if (emailCount == 1) {
                redis.expire(hourKey, 3600, TimeUnit.SECONDS);
            }

            if (emailCount > otpProperties.getLimit().getPerHour()) {
                Long remain = redis.getExpire(hourKey, TimeUnit.SECONDS);

                log.warn(
                        "WITHDRAW_OTP_RATE_LIMIT userId={} count={}",
                        userId,
                        emailCount
                );

                throw new OtpException(
                        MessageKeys.RATE_LIMIT_HOURLY,
                        remain != null ? remain.intValue() : null
                );
            }

            // ===== GENERATE NEW OTP =====
            String newOtp = generateOtp();

            // ===== UPDATE EXPIRATION (UTC) =====
            Instant newExpiresAt = Instant.now()
                    .plusSeconds(otpProperties.getTtlSeconds());

            order.setOtpExpiresAt(newExpiresAt);

            // ===== APPLY OTP AFTER DB COMMIT =====
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {

                        @Override
                        public void afterCommit() {
                            try {
                                Duration ttl = Duration.between(
                                        Instant.now(),
                                        newExpiresAt
                                );

                                if (ttl.isZero() || ttl.isNegative()) {
                                    return;
                                }

                                redis.opsForValue().set(
                                        otpKey,
                                        newOtp,
                                        ttl
                                );

                                mailService.sendOtp(email, newOtp);

                                log.info(
                                        "WITHDRAW_OTP_RESENT orderNo={} userId={}",
                                        orderNo,
                                        userId
                                );

                            } catch (Exception ex) {
                                log.error(
                                        "WITHDRAW_OTP_RESEND_FAILED orderNo={} userId={}",
                                        orderNo,
                                        userId,
                                        ex
                                );
                            }
                        }
                    }
            );

            return ResendOtpResult.RESENT;
        });

        if (result == ResendOtpResult.EXPIRED) {
            throw new WalletException(
                    MessageKeys.WITHDRAW_OTP_EXPIRED,
                    HttpStatus.GONE
            );
        }

        if (result != ResendOtpResult.RESENT) {
            throw new IllegalStateException("Withdraw OTP resend returned no valid result");
        }
    }

    private enum ResendOtpResult {
        RESENT,
        EXPIRED
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
                .orElseThrow(() ->
                        new WalletException(
                                MessageKeys.WALLET_WITHDRAW_SUMMARY_NOT_FOUND,
                                HttpStatus.NOT_FOUND
                        )
                );

        CryptoNetwork network = summary.getNetwork() == null
                ? null
                : CryptoNetwork.valueOf(summary.getNetwork());

        return WithdrawSummaryResponse.builder()
                .walletId(summary.getWalletId())
                .balance(safe(summary.getBalance()))
                .minimumWithdrawalAmount(safe(summary.getMinimumWithdrawalAmount()))
                .currency(summary.getCurrency())
                .network(network)
                .address(summary.getAddress())
                .build();
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private enum ConfirmOtpResult {
        CONFIRMED,
        INVALID,
        EXPIRED,
        TOO_MANY_ATTEMPTS,
        INSUFFICIENT_BALANCE
    }


    private TransactionTemplate newTransaction() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }

    private void cleanupWithdrawOtpAfterCommit(String orderNo) {

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        try {
                            redis.delete(List.of(
                                    RedisKeys.withdrawOtpValueKey(orderNo),
                                    RedisKeys.withdrawOtpAttemptKey(orderNo)
                            ));
                        } catch (Exception ex) {
                            log.error(
                                    "WITHDRAW_OTP_CLEANUP_FAILED orderNo={}",
                                    orderNo,
                                    ex
                            );
                        }
                    }
                }
        );
    }

}
