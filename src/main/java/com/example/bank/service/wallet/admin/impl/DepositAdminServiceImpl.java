package com.example.bank.service.wallet.admin.impl;

import com.example.bank.common.config.properties.WalletProperties;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.request.wallet.admin.UpdateDepositStatusRequest;
import com.example.bank.dto.response.wallet.admin.DepositOrderListAdminResponse;
import com.example.bank.dto.response.wallet.admin.DepositOrderPageAdminResponse;
import com.example.bank.entity.wallet.DepositOrder;
import com.example.bank.entity.wallet.Wallet;
import com.example.bank.enums.wallet.DepositOrderStatus;
import com.example.bank.repository.wallet.DepositOrderRepository;
import com.example.bank.repository.wallet.WalletRepository;
import com.example.bank.service.wallet.admin.DepositAdminService;
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
public class DepositAdminServiceImpl implements DepositAdminService {
    private final DepositOrderRepository depositOrderRepository;
    private final WalletRepository walletRepository;
    private final WalletProperties walletProperties;


    @Override
    @Transactional
    public void updateDepositStatus(
            String orderNo,
            UpdateDepositStatusRequest request
    ) {
        DepositOrder order = depositOrderRepository.findByOrderNoForUpdate(orderNo)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.DEPOSIT_ORDER_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        DepositOrderStatus currentStatus = order.getStatus();
        DepositOrderStatus newStatus = request.getStatus();

        /*
         * Chỉ cho xử lý lệnh đang PENDING.
         * SUCCESS / FAILED rồi thì không cho update lại.
         */
        if (currentStatus != DepositOrderStatus.PENDING) {
            throw new WalletException(
                    MessageKeys.DEPOSIT_INVALID_STATUS,
                    HttpStatus.BAD_REQUEST
            );
        }

        /*
         * Admin chỉ được chuyển sang SUCCESS hoặc FAILED.
         * Không cho update về PENDING.
         */
        if (newStatus != DepositOrderStatus.SUCCESS &&
                newStatus != DepositOrderStatus.FAILED) {
            throw new WalletException(
                    MessageKeys.DEPOSIT_INVALID_STATUS,
                    HttpStatus.BAD_REQUEST
            );
        }

        order.setAdminNote(request.getAdminNote());

        if (newStatus == DepositOrderStatus.SUCCESS) {
            Wallet wallet = walletRepository.findByUserIdForUpdate(order.getUserId())
                    .orElseThrow(() -> new WalletException(
                            MessageKeys.WALLET_NOT_FOUND,
                            HttpStatus.NOT_FOUND
                    ));

            /*
             * amount = số tiền user khai báo nạp
             * fee = phí
             * expectedAmount = amount - fee
             *
             * Khi admin duyệt thành công, cộng expectedAmount vào ví.
             */
            wallet.setAvailableBalance(
                    wallet.getAvailableBalance().add(order.getExpectedAmount())
            );

            wallet.setTotalBalance(
                    wallet.getTotalBalance().add(order.getExpectedAmount())
            );

            walletRepository.save(wallet);
            order.markSuccess();
        } else {
            /*
             * Deposit FAILED:
             * Không cộng tiền vào ví vì lệnh nạp chưa được credit.
             */
            order.markFailed();
        }

        depositOrderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public DepositOrderPageAdminResponse getAllDepositOrders(int page) {
        if (page < 0) {
            page = 0;
        }
        int size = walletProperties.getDefaultPageSize();

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "id")
        );

        Page<DepositOrder> orders =
                depositOrderRepository.findAll(pageable);

        if (orders.isEmpty()) {
            return DepositOrderPageAdminResponse.builder()
                    .items(List.of())
                    .page(page)
                    .size(pageable.getPageSize())
                    .totalSize(0)
                    .hasNext(false)
                    .build();
        }

        List<DepositOrderListAdminResponse> items =
                orders.getContent()
                        .stream()
                        .map(DepositOrderListAdminResponse::from)
                        .toList();

        return DepositOrderPageAdminResponse.builder()
                .items(items)
                .page(page)
                .size(pageable.getPageSize())
                .totalSize(orders.getTotalElements())
                .hasNext(orders.hasNext())
                .build();
    }
}
