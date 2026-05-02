package com.example.bank.service.wallet.admin;

import com.example.bank.dto.request.wallet.admin.UpdateDepositStatusRequest;
import com.example.bank.dto.response.wallet.admin.DepositOrderPageAdminResponse;

public interface DepositAdminService{
    void updateDepositStatus(
            String orderNo,
            UpdateDepositStatusRequest request
    );

    DepositOrderPageAdminResponse getAllDepositOrders(int page);
}
