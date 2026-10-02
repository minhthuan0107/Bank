package com.example.bank.controller.user.wallet;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.request.wallet.user.ConfirmWithdrawOtpRequest;
import com.example.bank.dto.request.wallet.user.CreateWithdrawOrderRequest;
import com.example.bank.dto.response.wallet.user.CreateWithdrawOrderResponse;
import com.example.bank.dto.response.wallet.user.WithdrawDashboardResponse;
import com.example.bank.dto.response.wallet.user.WithdrawOrderPageResponse;
import com.example.bank.dto.response.wallet.user.WithdrawSummaryResponse;
import com.example.bank.enums.wallet.WithdrawOrderStatus;
import com.example.bank.service.wallet.user.WithdrawService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("${api.prefix}/withdraw")
@RequiredArgsConstructor
public class WithdrawController {

    private final WithdrawService withdrawService;
    private final LocalizationUtils i18n;

    @PostMapping("/orders")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<CreateWithdrawOrderResponse>> createWithdrawOrder(
            @Valid @RequestBody CreateWithdrawOrderRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            HttpServletRequest httpServletRequest
    ) {

        CreateWithdrawOrderResponse response =
                withdrawService.createWithdrawOrder(
                        request,
                        currentUser.getId(),
                        httpServletRequest
                );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        HttpStatus.CREATED.value(),
                        i18n.getLocalizedMessage(MessageKeys.WITHDRAW_ORDER_CREATED),
                        response
                ));
    }



    @PostMapping("/confirm-otp")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> confirmWithdrawOtp(
            @Valid @RequestBody ConfirmWithdrawOtpRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        withdrawService.confirmWithdrawOtp(
                currentUser.getId(),
                request
        );
        return ResponseEntity.ok(
                ApiResponse.ok(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.WITHDRAW_OTP_CONFIRMED)
                )
        );
    }


    @PostMapping("/resend-otp")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> resendWithdrawOtp(
            @RequestParam String orderNo,
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        withdrawService.resendWithdrawOtp(
                currentUser.getId(),
                orderNo
        );

        return ResponseEntity.ok(
                ApiResponse.ok(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.OTP_RESENT)
                )
        );
    }

    @GetMapping("/orders-list")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<WithdrawOrderPageResponse>> getWithdrawOrders(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(defaultValue = "0") int page
    ) {

        WithdrawOrderPageResponse response =
                withdrawService.getUserWithdrawOrders(
                        currentUser.getId(),
                        page
                );
        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.WITHDRAW_ORDER_LIST_SUCCESS),
                        response
                )
        );
    }

    @GetMapping("/orders")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<WithdrawOrderPageResponse>> getWithdrawOrders(
            @AuthenticationPrincipal UserDetailsImpl currentUser,

            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) WithdrawOrderStatus status,

            @RequestParam(required = false) Instant fromTime,
            @RequestParam(required = false) Instant toTime,

            @RequestParam(defaultValue = "0") int page
    ) {
        WithdrawOrderPageResponse response =
                withdrawService.getWithdrawOrders(
                        currentUser.getId(),
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
                        i18n.getLocalizedMessage(MessageKeys.WITHDRAW_ORDER_LIST_SUCCESS),
                        response
                )
        );
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<WithdrawDashboardResponse>> getWithdrawDashboard(
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        WithdrawDashboardResponse response =
                withdrawService.getWithdrawDashboard(currentUser.getId());

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.WITHDRAW_DASHBOARD_SUCCESS),
                        response
                )
        );
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<WithdrawSummaryResponse>> getWithdrawSummary(
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        WithdrawSummaryResponse response =
                withdrawService.getWithdrawSummary(currentUser.getId());

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.WALLET_WITHDRAW_SUMMARY_SUCCESS),
                        response
                )
        );
    }

}