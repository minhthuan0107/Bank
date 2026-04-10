package com.example.bank.service.wallet.user;

import com.example.bank.dto.request.wallet.user.ConfirmWithdrawOtpRequest;
import com.example.bank.dto.request.wallet.user.CreateWithdrawOrderRequest;
import com.example.bank.dto.response.wallet.user.CreateWithdrawOrderResponse;
import com.example.bank.dto.response.wallet.user.WithdrawDashboardResponse;
import com.example.bank.dto.response.wallet.user.WithdrawOrderPageResponse;
import com.example.bank.enums.wallet.WithdrawOrderStatus;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;

public interface WithdrawService {

    CreateWithdrawOrderResponse createWithdrawOrder(
            CreateWithdrawOrderRequest request,
            Long userId,
            HttpServletRequest httpServletRequest
    );

    void confirmWithdrawOtp(
            Long userId,
            ConfirmWithdrawOtpRequest request
    );

    void resendWithdrawOtp(Long userId, String withdrawId);


    WithdrawOrderPageResponse getUserWithdrawOrders(
            Long userId,
            int page
    );

    WithdrawOrderPageResponse getWithdrawOrders(
            Long userId,
            String orderNo,
            String address,
            WithdrawOrderStatus status,
            Instant fromTime,
            Instant toTime,
            int page
    );

    WithdrawDashboardResponse getWithdrawDashboard(Long userId);





}