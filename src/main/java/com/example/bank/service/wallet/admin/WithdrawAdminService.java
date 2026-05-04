package com.example.bank.service.wallet.admin;

import com.example.bank.dto.request.wallet.admin.UpdateWithdrawStatusRequest;
import com.example.bank.dto.response.wallet.admin.WithdrawOrderPageAdminResponse;
import com.example.bank.dto.response.wallet.user.WithdrawOrderPageResponse;
import com.example.bank.enums.wallet.WithdrawOrderStatus;

import java.time.Instant;

public interface WithdrawAdminService {
    void updateWithdrawStatus(
            String orderNo,
            UpdateWithdrawStatusRequest request
    );

    WithdrawOrderPageAdminResponse getAllWithdrawOrders(
            String orderNo,
            String username,
            WithdrawOrderStatus status,
            Instant fromTime,
            Instant toTime,
            int page
    );
}
