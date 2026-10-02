package com.example.bank.controller.admin.wallet;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.request.wallet.admin.UpdateWithdrawStatusRequest;
import com.example.bank.dto.response.wallet.admin.WithdrawOrderPageAdminResponse;
import com.example.bank.dto.response.wallet.user.WithdrawOrderPageResponse;
import com.example.bank.enums.wallet.WithdrawOrderStatus;
import com.example.bank.service.wallet.admin.WithdrawAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

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
    public ResponseEntity<ApiResponse<WithdrawOrderPageAdminResponse>> getAllWithdrawOrders(
            @RequestParam(name = "order_no", required = false) String orderNo,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) WithdrawOrderStatus status,
            @RequestParam(name = "from_time", required = false) Instant fromTime,
            @RequestParam(name = "to_time", required = false) Instant toTime,
            @RequestParam(defaultValue = "0") int page
    ) {
        WithdrawOrderPageAdminResponse response =
                service.getAllWithdrawOrders(
                        orderNo,
                        username,
                        status,
                        fromTime,
                        toTime,
                        page
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.ADMIN_WITHDRAW_ORDER_LIST_SUCCESS),
                        response
                )
        );
    }

    @GetMapping("/users/{userId}/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WithdrawOrderPageAdminResponse>> getUserWithdrawOrders(
            @PathVariable Long userId,
            @RequestParam(name = "order_no", required = false) String orderNo,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) WithdrawOrderStatus status,
            @RequestParam(name = "from_time", required = false) Instant fromTime,
            @RequestParam(name = "to_time", required = false) Instant toTime,
            @RequestParam(defaultValue = "0") int page
    ) {
        WithdrawOrderPageAdminResponse response =
                service.getUserWithdrawOrders(
                        userId,
                        orderNo,
                        address,
                        status,
                        fromTime,
                        toTime,
                        page
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(
                                MessageKeys.ADMIN_WITHDRAW_ORDER_LIST_SUCCESS
                        ),
                        response
                )
        );
    }
}
