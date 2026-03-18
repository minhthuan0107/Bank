package com.example.bank.service.wallet.user;

import com.example.bank.dto.request.wallet.user.ConfirmWithdrawOtpRequest;
import com.example.bank.dto.request.wallet.user.CreateWithdrawOrderRequest;
import com.example.bank.dto.response.wallet.user.CreateWithdrawOrderResponse;

public interface WithdrawService {

    CreateWithdrawOrderResponse createWithdrawOrder(
            CreateWithdrawOrderRequest request,
            Long userId
    );

    void confirmWithdrawOtp(
            Long userId,
            ConfirmWithdrawOtpRequest request
    );

    void resendWithdrawOtp(Long userId, String withdrawId);

}