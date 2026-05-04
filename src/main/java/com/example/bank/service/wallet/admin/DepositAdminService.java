package com.example.bank.service.wallet.admin;

import com.example.bank.dto.request.wallet.admin.UpdateDepositStatusRequest;
import com.example.bank.dto.response.wallet.admin.DepositOrderPageAdminResponse;
import com.example.bank.enums.wallet.DepositOrderStatus;

import java.time.Instant;

public interface DepositAdminService{
    void updateDepositStatus(
            String orderNo,
            UpdateDepositStatusRequest request
    );


    DepositOrderPageAdminResponse getAllDepositOrders(
            String orderNo,
            String username,
            DepositOrderStatus status,
            Instant fromTime,
            Instant toTime,
            int page
    );
}
