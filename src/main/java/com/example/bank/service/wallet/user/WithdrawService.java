package com.example.bank.service.wallet.user;

import com.example.bank.dto.request.wallet.user.ConfirmWithdrawOtpRequest;
import com.example.bank.dto.request.wallet.user.CreateWithdrawOrderRequest;
import com.example.bank.dto.response.wallet.user.CreateWithdrawOrderResponse;
import com.example.bank.dto.response.wallet.user.WithdrawOrderPageResponse;
import jakarta.servlet.http.HttpServletRequest;

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



}