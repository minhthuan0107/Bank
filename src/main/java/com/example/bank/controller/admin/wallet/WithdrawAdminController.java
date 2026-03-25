package com.example.bank.controller.admin.wallet;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.request.wallet.admin.UpdateWithdrawStatusRequest;
import com.example.bank.dto.response.wallet.user.WithdrawOrderPageResponse;
import com.example.bank.service.wallet.admin.WithdrawAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/admin/withdraw")
@RequiredArgsConstructor
public class WithdrawAdminController {

    private final WithdrawAdminService service;
    private final LocalizationUtils i18n;

    @PatchMapping("/{orderNo}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateWithdrawStatus(
            @PathVariable String orderNo,
            @Valid @RequestBody UpdateWithdrawStatusRequest request
    ) {
        service.updateWithdrawStatus(orderNo, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.WITHDRAW_STATUS_UPDATED),
                        null
                )
        );
    }

    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WithdrawOrderPageResponse>> getAllWithdrawOrders(
            @RequestParam(defaultValue = "0") int page
    ) {
        WithdrawOrderPageResponse response =
                service.getAllWithdrawOrders(page);
        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.ADMIN_WITHDRAW_ORDER_LIST_SUCCESS),
                        response
                )
        );
    }
}
