package com.example.bank.service.wallet.user;

import com.example.bank.dto.request.wallet.user.CreateDepositOrderRequest;
import com.example.bank.dto.request.wallet.user.DepositPreviewRequest;
import com.example.bank.dto.response.wallet.user.CreateDepositOrderResponse;
import com.example.bank.dto.response.wallet.user.DepositConfigResponse;
import com.example.bank.dto.response.wallet.user.DepositPreviewResponse;
import com.example.bank.enums.wallet.Stablecoin;

public interface DepositService {
    DepositConfigResponse getDepositConfig(Stablecoin currency , Long userId);

    DepositPreviewResponse previewDeposit(DepositPreviewRequest request);

    CreateDepositOrderResponse createDepositOrder(
            CreateDepositOrderRequest request,
            Long userId
    );
}
