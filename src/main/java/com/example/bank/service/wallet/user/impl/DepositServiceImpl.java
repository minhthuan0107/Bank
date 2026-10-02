package com.example.bank.service.wallet.user.impl;

import com.example.bank.common.config.r2.R2Service;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.constants.RedisKeys;
import com.example.bank.common.exception.auth.OtpException;
import com.example.bank.common.exception.user.UserException;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.wallet.user.CreateDepositOrderRequest;
import com.example.bank.dto.request.wallet.user.DepositPreviewRequest;
import com.example.bank.dto.response.wallet.user.*;
import com.example.bank.entity.user.User;
import com.example.bank.entity.wallet.*;
import com.example.bank.enums.user.AccountStatus;
import com.example.bank.enums.wallet.DepositOrderStatus;
import com.example.bank.enums.wallet.Stablecoin;
import com.example.bank.event.DepositOrderCreatedEvent;
import com.example.bank.repository.projection.DepositDashboardProjection;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.repository.wallet.*;
import com.example.bank.service.wallet.user.DepositService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DepositServiceImpl implements DepositService {

    private final WalletCurrencySettingsRepository depositSettingsRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final DepositAddressRepository depositAddressRepository;
    private final DepositOrderRepository depositOrderRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final R2Service r2Service;
    private final DepositOrderImageRepository depositOrderImageRepository;

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_DEPOSIT_PER_HOUR = 10;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final int MAX_IMAGES = 5;
    private static final List<String> ALLOWED_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private final ApplicationEventPublisher applicationEventPublisher;
    private final WalletExternalAddressRepository walletExternalAddressRepository;


    @Override
    public DepositConfigResponse getDepositConfig(Stablecoin currency , Long userId) {
        WalletCurrencySettings settings = depositSettingsRepository
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
                .availableBalance(wallet.getAvailableBalance()) // số dư thực tế có thể dùng
                .feePercent(settings.getDepositFeePercent())
                .minAmount(settings.getMinDepositAmount())
                .build();
    }

    @Override
    public DepositPreviewResponse previewDeposit(DepositPreviewRequest request) {
        String network = request.getNetwork() != null
                ? request.getNetwork()
                : "TRC20";

        WalletCurrencySettings settings = depositSettingsRepository
                .findByCurrency(request.getCurrency())
                .orElseThrow(() ->
                        new WalletException(
                                MessageKeys.DEPOSIT_SETTINGS_NOT_FOUND,
                                HttpStatus.NOT_FOUND
                        )
                );

        if (request.getAmount().compareTo(settings.getMinDepositAmount()) < 0) {
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
                        .multiply(settings.getDepositFeePercent())
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

        // ===== GET USER EXTERNAL ADDRESS =====
        WalletExternalAddress externalAddress =
                walletExternalAddressRepository
                        .findByUserId(userId)
                        .orElseThrow(() ->
                                new WalletException(
                                        MessageKeys.WALLET_EXTERNAL_ADDRESS_REQUIRED,
                                        HttpStatus.BAD_REQUEST
                                )
                        );

        WalletCurrencySettings settings = depositSettingsRepository
                .findByCurrency(request.getCurrency())
                .orElseThrow(() ->
                        new WalletException(
                                MessageKeys.DEPOSIT_SETTINGS_NOT_FOUND,
                                HttpStatus.NOT_FOUND
                        )
                );

        if (request.getAmount().compareTo(settings.getMinDepositAmount()) < 0) {
            throw new WalletException(
                    MessageKeys.DEPOSIT_AMOUNT_BELOW_MIN,
                    HttpStatus.BAD_REQUEST
            );
        }

        DepositAddress depositAddress = depositAddressRepository
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
                .multiply(settings.getDepositFeePercent())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        BigDecimal expectedAmount = request.getAmount().subtract(fee);

        String orderNo = generateOrderNo();

        DepositOrder order = DepositOrder.create(
                userId,
                orderNo,
                request.getCurrency(),

                // Network cố định của user
                externalAddress.getNetwork().name(),

                // Địa chỉ Bank nhận tiền
                depositAddress.getAddress(),

                // Địa chỉ user gửi tiền
                externalAddress.getAddress(),

                request.getAmount(),
                fee,
                expectedAmount
        );

        depositOrderRepository.save(order);

        applicationEventPublisher.publishEvent(new DepositOrderCreatedEvent(
                order.getId(),
                userId,
                order.getOrderNo(),
                order.getCurrency(),
                order.getNetwork(),
                order.getAmount(),
                order.getFee(),
                order.getExpectedAmount(),
                order.getSourceAddress()
        ));

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

    @Transactional
    public void uploadDepositImages(
            String orderNo,
            List<MultipartFile> images,
            Long userId
    ) {

        validateImagesRequest(images);

        DepositOrder order = depositOrderRepository
                .findByOrderNoAndUserId(orderNo, userId)
                .orElseThrow(() ->
                        new WalletException(
                                MessageKeys.DEPOSIT_ORDER_NOT_FOUND,
                                HttpStatus.NOT_FOUND
                        )
                );

        validateOrderForUpload(order);

        List<DepositOrderImage> entities = new ArrayList<>();
        int displayOrder = 1;

        for (MultipartFile file : images) {

            validateImage(file);

            String url = r2Service.upload(file, orderNo);

            DepositOrderImage image = new DepositOrderImage();
            image.setDepositOrderId(order.getId());
            image.setImageUrl(url);
            image.setDisplayOrder(displayOrder++);

            entities.add(image);
        }

        depositOrderImageRepository.saveAll(entities);
    }

    private void validateImagesRequest(List<MultipartFile> images) {

        if (images == null || images.isEmpty()) {
            throw new WalletException(
                    MessageKeys.DEPOSIT_PROOF_REQUIRED,
                    HttpStatus.BAD_REQUEST
            );
        }

        if (images.size() > MAX_IMAGES) {
            throw new WalletException(
                    MessageKeys.DEPOSIT_PROOF_MAX_IMAGES,
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private void validateOrderForUpload(DepositOrder order) {

        if (order.getStatus() != DepositOrderStatus.PENDING) {
            throw new WalletException(
                    MessageKeys.DEPOSIT_ORDER_INVALID_STATUS,
                    HttpStatus.CONFLICT
            );
        }
        boolean exists = depositOrderImageRepository
                .existsByDepositOrderId(order.getId());
        if (exists) {
            throw new WalletException(
                    MessageKeys.DEPOSIT_PROOF_ALREADY_UPLOADED,
                    HttpStatus.CONFLICT
            );
        }
    }

    private void validateImage(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new WalletException(
                    MessageKeys.FILE_EMPTY,
                    HttpStatus.BAD_REQUEST
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new WalletException(
                    MessageKeys.FILE_TOO_LARGE,
                    HttpStatus.PAYLOAD_TOO_LARGE
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new WalletException(
                    MessageKeys.FILE_INVALID_TYPE,
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE
            );
        }

        // kiểm tra file có thực sự là ảnh
        try (InputStream is = file.getInputStream()) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                throw new WalletException(
                        MessageKeys.FILE_INVALID_IMAGE,
                        HttpStatus.BAD_REQUEST
                );
            }

        } catch (IOException e) {
            throw new WalletException(
                    MessageKeys.FILE_READ_ERROR,
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @Override
    public DepositOrderPageResponse getUserDepositOrders(
            Long userId,
            int page
    ) {

        Pageable pageable = PageRequest.of(
                page,
                DEFAULT_PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "id")
        );

        Page<DepositOrder> orders =
                depositOrderRepository.findByUserId(userId, pageable);

        List<DepositOrderListResponse> items =
                orders.getContent()
                        .stream()
                        .map(DepositOrderListResponse::from)
                        .toList();

        return DepositOrderPageResponse.builder()
                .items(items)
                .page(page)
                .size(DEFAULT_PAGE_SIZE)
                .totalSize(orders.getTotalElements())
                .hasNext(orders.hasNext())
                .build();
    }

    public DepositOrderPageResponse getDepositOrders(
            Long userId,
            String orderNo,
            String address,
            DepositOrderStatus status,
            Instant fromTime,
            Instant toTime,
            int page
    ) {
        //normalize
        orderNo = normalize(orderNo);
        address = normalize(address);
        //validate time
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

        Page<DepositOrder> result = depositOrderRepository.search(
                userId,
                orderNo,
                address,
                status,
                fromTime,
                toTime,
                pageable
        );

        List<DepositOrderListResponse> items = result.getContent()
                .stream()
                .map(DepositOrderListResponse::from)
                .toList();

        return DepositOrderPageResponse.builder()
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
    public DepositDashboardResponse getDepositDashboard(Long userId) {
        DepositDashboardProjection p = depositOrderRepository.getDashboard(userId);
        return DepositDashboardResponse.builder()
                .totalAmount(
                        p != null && p.getTotalAmount() != null
                                ? p.getTotalAmount().stripTrailingZeros()
                                : BigDecimal.ZERO
                )
                .pendingCount(
                        p != null && p.getPendingCount() != null
                                ? p.getPendingCount()
                                : 0L
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

}
