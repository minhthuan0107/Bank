package com.example.bank.service.wallet.admin.impl;

import com.example.bank.common.config.properties.WalletProperties;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.wallet.admin.UpdateWithdrawStatusRequest;
import com.example.bank.dto.response.wallet.admin.WithdrawOrderListAdminResponse;
import com.example.bank.dto.response.wallet.admin.WithdrawOrderPageAdminResponse;
import com.example.bank.dto.response.wallet.user.WithdrawOrderListResponse;
import com.example.bank.dto.response.wallet.user.WithdrawOrderPageResponse;
import com.example.bank.entity.wallet.Wallet;
import com.example.bank.entity.wallet.WithdrawOrder;
import com.example.bank.enums.wallet.WithdrawOrderStatus;
import com.example.bank.projection.UserNameProjection;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.repository.wallet.WalletRepository;
import com.example.bank.repository.wallet.WithdrawOrderRepository;
import com.example.bank.service.wallet.admin.WithdrawAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WithdrawAdminServiceImpl implements WithdrawAdminService {
    private final WithdrawOrderRepository withdrawOrderRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final WalletProperties walletProperties;

    @Transactional
    public void updateWithdrawStatus(
            String orderNo,
            UpdateWithdrawStatusRequest request
    ) {
        WithdrawOrder order = withdrawOrderRepository.findByOrderNoForUpdate(orderNo)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.WITHDRAW_ORDER_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        WithdrawOrderStatus currentStatus = order.getStatus();
        WithdrawOrderStatus newStatus = request.getStatus();

        if (currentStatus != WithdrawOrderStatus.PENDING_ADMIN) {
            throw new WalletException(
                    MessageKeys.WITHDRAW_INVALID_STATUS,
                    HttpStatus.BAD_REQUEST
            );
        }

        if (newStatus != WithdrawOrderStatus.SUCCESS &&
                newStatus != WithdrawOrderStatus.FAILED) {
            throw new WalletException(
                    MessageKeys.WITHDRAW_INVALID_STATUS,
                    HttpStatus.BAD_REQUEST
            );
        }

        if (newStatus == WithdrawOrderStatus.SUCCESS) {
            order.markSuccess();
        } else {
            Wallet wallet = walletRepository.findByUserIdForUpdate(order.getUserId())
                    .orElseThrow(() -> new WalletException(
                            MessageKeys.WALLET_NOT_FOUND,
                            HttpStatus.NOT_FOUND
                    ));
            wallet.setAvailableBalance(
                    wallet.getAvailableBalance().add(order.getAmount())
            );
            wallet.setTotalBalance(
                    wallet.getTotalBalance().add(order.getAmount())
            );
            walletRepository.save(wallet);
            order.setAdminNote(request.getAdminNote());
            order.markFailed();
        }
        withdrawOrderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public WithdrawOrderPageAdminResponse getAllWithdrawOrders(
            String orderNo,
            String username,
            WithdrawOrderStatus status,
            Instant fromTime,
            Instant toTime,
            int page
    ) {
        // normalize
        orderNo = normalize(orderNo);
        username = normalize(username);

        // validate page
        if (page < 0) {
            throw new WalletException(
                    MessageKeys.INVALID_PAGE_NUMBER,
                    HttpStatus.BAD_REQUEST
            );
        }

        // validate time
        if (fromTime != null && toTime != null && fromTime.isAfter(toTime)) {
            throw new WalletException(
                    MessageKeys.INVALID_TIME_RANGE,
                    HttpStatus.BAD_REQUEST
            );
        }

        List<WithdrawOrderStatus> adminStatuses = List.of(
                WithdrawOrderStatus.PENDING_ADMIN,
                WithdrawOrderStatus.SUCCESS,
                WithdrawOrderStatus.FAILED
        );

        // Nếu admin truyền status thì status đó phải thuộc nhóm admin được xem
        if (status != null && !adminStatuses.contains(status)) {
            throw new WalletException(
                    MessageKeys.WITHDRAW_INVALID_STATUS,
                    HttpStatus.BAD_REQUEST
            );
        }

        int size = walletProperties.getDefaultPageSize();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );



        Page<WithdrawOrder> orders =
                withdrawOrderRepository.searchAdminOrders(
                        adminStatuses,
                        orderNo,
                        username,
                        status,
                        fromTime,
                        toTime,
                        pageable
                );

        List<WithdrawOrder> content = orders.getContent();

        if (content.isEmpty()) {
            return WithdrawOrderPageAdminResponse.builder()
                    .items(List.of())
                    .page(page)
                    .size(pageable.getPageSize())
                    .totalSize(0)
                    .hasNext(false)
                    .build();
        }

        List<Long> userIds = content.stream()
                .map(WithdrawOrder::getUserId)
                .distinct()
                .toList();

        Map<Long, String> usernameMap = userRepository.findByIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(
                        UserNameProjection::getId,
                        UserNameProjection::getUsername
                ));

        List<WithdrawOrderListAdminResponse> items =
                content.stream()
                        .map(order -> WithdrawOrderListAdminResponse.from(
                                order,
                                usernameMap.get(order.getUserId())
                        ))
                        .toList();

        return WithdrawOrderPageAdminResponse.builder()
                .items(items)
                .page(page)
                .size(pageable.getPageSize())
                .totalSize(orders.getTotalElements())
                .hasNext(orders.hasNext())
                .build();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @Override
    @Transactional(readOnly = true)
    public WithdrawOrderPageAdminResponse getUserWithdrawOrders(
            Long userId,
            String orderNo,
            String address,
            WithdrawOrderStatus status,
            Instant fromTime,
            Instant toTime,
            int page
    ) {
        orderNo = normalize(orderNo);
        address = normalize(address);

        if (page < 0) {
            throw new WalletException(
                    MessageKeys.INVALID_PAGE_NUMBER,
                    HttpStatus.BAD_REQUEST
            );
        }

        if (fromTime != null && toTime != null && fromTime.isAfter(toTime)) {
            throw new WalletException(
                    MessageKeys.INVALID_TIME_RANGE,
                    HttpStatus.BAD_REQUEST
            );
        }

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.USER_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        int size = walletProperties.getDefaultPageSize();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );

        Page<WithdrawOrder> orders = withdrawOrderRepository.search(
                userId,
                orderNo,
                address,
                status,
                fromTime,
                toTime,
                pageable
        );

        List<WithdrawOrderListAdminResponse> items = orders.getContent()
                .stream()
                .map(order -> WithdrawOrderListAdminResponse.from(
                        order,
                        user.getUsername()
                ))
                .toList();

        return WithdrawOrderPageAdminResponse.builder()
                .items(items)
                .page(page)
                .size(pageable.getPageSize())
                .totalSize(orders.getTotalElements())
                .hasNext(orders.hasNext())
                .build();
    }


}
