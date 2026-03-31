package com.example.bank.service.wallet.admin.impl;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.wallet.admin.UpdateWithdrawStatusRequest;
import com.example.bank.dto.response.wallet.user.WithdrawOrderListResponse;
import com.example.bank.dto.response.wallet.user.WithdrawOrderPageResponse;
import com.example.bank.entity.wallet.Wallet;
import com.example.bank.entity.wallet.WithdrawOrder;
import com.example.bank.enums.wallet.WithdrawOrderStatus;
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

import java.util.List;

@Service
@RequiredArgsConstructor
public class WithdrawAdminServiceImpl implements WithdrawAdminService {
    private final WithdrawOrderRepository withdrawOrderRepository;
    private final WalletRepository walletRepository;
    private static final int DEFAULT_PAGE_SIZE = 10;

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
    public WithdrawOrderPageResponse getAllWithdrawOrders(int page) {
        if (page < 0) {
            throw new WalletException(
                    MessageKeys.INVALID_PAGE_NUMBER,
                    HttpStatus.BAD_REQUEST
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                DEFAULT_PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        List<WithdrawOrderStatus> adminStatuses = List.of(
                WithdrawOrderStatus.PENDING_ADMIN,
                WithdrawOrderStatus.SUCCESS,
                WithdrawOrderStatus.FAILED
        );

        Page<WithdrawOrder> orders =
                withdrawOrderRepository.findAdminOrders(adminStatuses, pageable);

        List<WithdrawOrderListResponse> items =
                orders.getContent()
                        .stream()
                        .map(WithdrawOrderListResponse::from)
                        .toList();

        return WithdrawOrderPageResponse.builder()
                .items(items)
                .page(page)
                .size(pageable.getPageSize())
                .totalSize(orders.getTotalElements())
                .hasNext(orders.hasNext())
                .build();
    }
}
