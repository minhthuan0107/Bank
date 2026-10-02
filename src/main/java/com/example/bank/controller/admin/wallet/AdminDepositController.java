package com.example.bank.controller.admin.wallet;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.request.wallet.admin.UpdateDepositStatusRequest;
import com.example.bank.dto.response.wallet.admin.DepositOrderPageAdminResponse;
import com.example.bank.enums.wallet.DepositOrderStatus;
import com.example.bank.service.wallet.admin.DepositAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

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
            @RequestParam(name = "order_no", required = false) String orderNo,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) DepositOrderStatus status,
            @RequestParam(name = "from_time", required = false) Instant fromTime,
            @RequestParam(name = "to_time", required = false) Instant toTime,
            @RequestParam(defaultValue = "0") int page
    ) {
        DepositOrderPageAdminResponse response =
                service.getAllDepositOrders(
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
                        i18n.getLocalizedMessage(MessageKeys.ADMIN_DEPOSIT_ORDER_LIST_SUCCESS),
                        response
                )
        );
    }


    @GetMapping("/users/{userId}/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DepositOrderPageAdminResponse>> getUserDepositOrders(
            @PathVariable Long userId,
            @RequestParam(name = "order_no", required = false) String orderNo,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) DepositOrderStatus status,
            @RequestParam(name = "from_time", required = false) Instant fromTime,
            @RequestParam(name = "to_time", required = false) Instant toTime,
            @RequestParam(defaultValue = "0") int page
    ) {
        DepositOrderPageAdminResponse response =
                service.getUserDepositOrders(
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
                                MessageKeys.ADMIN_DEPOSIT_ORDER_LIST_SUCCESS
                        ),
                        response
                )
        );
    }

}