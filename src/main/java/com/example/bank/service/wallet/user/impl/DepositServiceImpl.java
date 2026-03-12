package com.example.bank.service.wallet.user.impl;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.constants.RedisKeys;
import com.example.bank.common.exception.auth.OtpException;
import com.example.bank.common.exception.user.UserException;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.wallet.user.CreateDepositOrderRequest;
import com.example.bank.dto.request.wallet.user.DepositPreviewRequest;
import com.example.bank.dto.response.wallet.user.CreateDepositOrderResponse;
import com.example.bank.dto.response.wallet.user.DepositConfigResponse;
import com.example.bank.dto.response.wallet.user.DepositPreviewResponse;
import com.example.bank.entity.user.User;
import com.example.bank.entity.wallet.DepositAddress;
import com.example.bank.entity.wallet.DepositOrder;
import com.example.bank.entity.wallet.DepositSettings;
import com.example.bank.entity.wallet.Wallet;
import com.example.bank.enums.user.AccountStatus;
import com.example.bank.enums.wallet.Stablecoin;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.repository.wallet.DepositAddressRepository;
import com.example.bank.repository.wallet.DepositOrderRepository;
import com.example.bank.repository.wallet.DepositSettingsRepository;
import com.example.bank.repository.wallet.WalletRepository;
import com.example.bank.service.wallet.user.DepositService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DepositServiceImpl implements DepositService {

    private final DepositSettingsRepository depositSettingsRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final DepositAddressRepository depositAddressRepository;
    private final DepositOrderRepository depositOrderRepository;
    private final StringRedisTemplate stringRedisTemplate;

    private static final int MAX_DEPOSIT_PER_HOUR = 10;


    @Override
    public DepositConfigResponse getDepositConfig(Stablecoin currency , Long userId) {

        DepositSettings settings = depositSettingsRepository
                .findByCurrencyAndStatus(currency, "ACTIVE")
                .orElseThrow(() ->
                        new WalletException(
                                MessageKeys.DEPOSIT_SETTINGS_NOT_FOUND,
                                HttpStatus.NOT_FOUND
                        )
                );

        Wallet wallet = walletRepository
                .findByUserIdAndCurrency(userId, currency)
                .orElseThrow(() ->
                        new WalletException(
                                MessageKeys.WALLET_NOT_FOUND,
                                HttpStatus.NOT_FOUND
                        )
                );

        return DepositConfigResponse.builder()
                .balance(wallet.getBalance())
                .feePercent(settings.getFeePercent())
                .minAmount(settings.getMinAmount())
                .maxAmount(settings.getMaxAmount())
                .build();
    }

    @Override
    public DepositPreviewResponse previewDeposit(DepositPreviewRequest request) {
        String network = request.getNetwork() != null
                ? request.getNetwork()
                : "TRC20";

        DepositSettings settings = depositSettingsRepository
                .findByCurrency(request.getCurrency())
                .orElseThrow(() ->
                        new WalletException(
                                MessageKeys.DEPOSIT_SETTINGS_NOT_FOUND,
                                HttpStatus.NOT_FOUND
                        )
                );

        if (request.getAmount().compareTo(settings.getMinAmount()) < 0) {
            throw new WalletException(
                    MessageKeys.DEPOSIT_AMOUNT_BELOW_MIN,
                    HttpStatus.BAD_REQUEST
            );
        }



        DepositAddress address = depositAddressRepository
                .findFirstByCurrencyAndNetworkAndStatus(
                        request.getCurrency(),
                        network,
                        "ACTIVE"
                )
                .orElseThrow(() ->
                        new WalletException(
                                MessageKeys.DEPOSIT_ADDRESS_NOT_FOUND,
                                HttpStatus.NOT_FOUND
                        )
                );

        // Tính tiền phí
        BigDecimal fee =
                request.getAmount()
                        .multiply(settings.getFeePercent())
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        //Số tiền nhận được
        BigDecimal expectedAmount =
                request.getAmount().subtract(fee);

        User admin = userRepository
                .findActiveAdmin(AccountStatus.ACTIVE)
                .orElseThrow(() ->
                        new UserException(
                                MessageKeys.ADMIN_NOT_FOUND,
                                HttpStatus.NOT_FOUND
                        )
                );

        return DepositPreviewResponse.from(
                admin,
                address,
                request,
                fee,
                expectedAmount
        );
    }


    @Transactional
    @Override
    public CreateDepositOrderResponse createDepositOrder(
            CreateDepositOrderRequest request,
            Long userId
    ) {

        checkDepositLimit(userId);

        DepositSettings settings = depositSettingsRepository
                .findByCurrency(request.getCurrency())
                .orElseThrow(() ->
                        new WalletException(
                                MessageKeys.DEPOSIT_SETTINGS_NOT_FOUND,
                                HttpStatus.NOT_FOUND
                        )
                );

        if (request.getAmount().compareTo(settings.getMinAmount()) < 0) {
            throw new WalletException(
                    MessageKeys.DEPOSIT_AMOUNT_BELOW_MIN,
                    HttpStatus.BAD_REQUEST
            );
        }

        DepositAddress address = depositAddressRepository
                .findFirstByCurrencyAndNetworkAndStatus(
                        request.getCurrency(),
                        request.getNetwork(),
                        "ACTIVE"
                )
                .orElseThrow(() ->
                        new WalletException(
                                MessageKeys.DEPOSIT_ADDRESS_NOT_FOUND,
                                HttpStatus.NOT_FOUND
                        )
                );

        BigDecimal fee = request.getAmount()
                .multiply(settings.getFeePercent())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        BigDecimal expectedAmount = request.getAmount().subtract(fee);

        String orderNo = generateOrderNo();

        DepositOrder order = DepositOrder.create(
                userId,
                orderNo,
                request.getCurrency(),
                request.getNetwork(),
                address.getAddress(),
                request.getAmount(),
                fee,
                expectedAmount
        );

        depositOrderRepository.save(order);

        return CreateDepositOrderResponse.from(order);
    }

    private String generateOrderNo() {
        return "DP"
                + System.currentTimeMillis()
                + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    private void checkDepositLimit(Long userId) {
        String key = RedisKeys.depositOrderLimit(userId);

        Long newCount = stringRedisTemplate.opsForValue().increment(key);

        if (newCount != null && newCount == 1) {
            stringRedisTemplate.expire(key, 1, TimeUnit.HOURS);
        }

        if (newCount != null && newCount > MAX_DEPOSIT_PER_HOUR) {
            long remain = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);

            throw new OtpException(
                    MessageKeys.DEPOSIT_ORDER_RATE_LIMIT,
                    remain > 0 ? (int) remain : null
            );
        }
    }
}
