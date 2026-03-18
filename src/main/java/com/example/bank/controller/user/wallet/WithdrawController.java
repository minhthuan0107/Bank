package com.example.bank.controller.user.wallet;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.dto.request.wallet.user.ConfirmWithdrawOtpRequest;
import com.example.bank.dto.request.wallet.user.CreateWithdrawOrderRequest;
import com.example.bank.dto.response.wallet.user.CreateWithdrawOrderResponse;
import com.example.bank.service.wallet.user.WithdrawService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/withdraw")
@RequiredArgsConstructor
public class WithdrawController {

    private final WithdrawService withdrawService;

    @PostMapping("/orders")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<CreateWithdrawOrderResponse>> createWithdrawOrder(
            @Valid @RequestBody CreateWithdrawOrderRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {

        CreateWithdrawOrderResponse response =
                withdrawService.createWithdrawOrder(
                        request,
                        currentUser.getId()
                );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        HttpStatus.CREATED.value(),
                        MessageKeys.WITHDRAW_ORDER_CREATED,
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
                        MessageKeys.WITHDRAW_OTP_CONFIRMED
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
                        MessageKeys.OTP_RESENT
                )
        );
    }
}