package com.example.bank.service.wallet.user;

import com.example.bank.dto.request.wallet.user.CreateDepositOrderRequest;
import com.example.bank.dto.request.wallet.user.DepositPreviewRequest;
import com.example.bank.dto.response.wallet.user.*;
import com.example.bank.enums.wallet.Stablecoin;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DepositService {
    DepositConfigResponse getDepositConfig(Stablecoin currency , Long userId);

    DepositPreviewResponse previewDeposit(DepositPreviewRequest request);

    CreateDepositOrderResponse createDepositOrder(
            CreateDepositOrderRequest request,
            Long userId
    );

    void uploadDepositImages(String orderNo, List<MultipartFile> images, Long userId);

    DepositOrderPageResponse getUserDepositOrders(
            Long userId,
            int page
    );
}
