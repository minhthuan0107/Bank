package com.example.bank.controller.admin.wallet;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.request.wallet.admin.UpdateDepositStatusRequest;
import com.example.bank.dto.response.wallet.admin.DepositOrderPageAdminResponse;
import com.example.bank.service.wallet.admin.DepositAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/admin/deposits")
public class AdminDepositController {
    private final DepositAdminService service;
    private final LocalizationUtils i18n;

    /**
     * Admin cập nhật trạng thái lệnh nạp tiền
     */
    @PatchMapping("/{orderNo}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateDepositStatus(
            @PathVariable String orderNo,
            @Valid @RequestBody UpdateDepositStatusRequest request
    ) {
        service.updateDepositStatus(orderNo, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.DEPOSIT_STATUS_UPDATED),
                        null
                )
        );
    }

    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DepositOrderPageAdminResponse>> getAllDepositOrders(
            @RequestParam(defaultValue = "0") int page
    ) {
        DepositOrderPageAdminResponse response =
                service.getAllDepositOrders(page);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.ADMIN_DEPOSIT_ORDER_LIST_SUCCESS),
                        response
                )
        );
    }
}