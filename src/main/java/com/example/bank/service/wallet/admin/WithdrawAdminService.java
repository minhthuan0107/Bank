package com.example.bank.service.wallet.admin;

import com.example.bank.dto.request.wallet.admin.UpdateWithdrawStatusRequest;
import com.example.bank.dto.response.wallet.user.WithdrawOrderPageResponse;

public interface WithdrawAdminService {
    void updateWithdrawStatus(
            String orderNo,
            UpdateWithdrawStatusRequest request
    );

    WithdrawOrderPageResponse getAllWithdrawOrders(int page);
}
