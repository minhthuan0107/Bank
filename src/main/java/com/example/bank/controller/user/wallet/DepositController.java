package com.example.bank.controller.user.wallet;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.dto.request.wallet.user.CreateDepositOrderRequest;
import com.example.bank.dto.request.wallet.user.DepositPreviewRequest;
import com.example.bank.dto.response.wallet.user.CreateDepositOrderResponse;
import com.example.bank.dto.response.wallet.user.DepositConfigResponse;
import com.example.bank.dto.response.wallet.user.DepositPreviewResponse;
import com.example.bank.enums.wallet.Stablecoin;
import com.example.bank.service.wallet.user.DepositService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("${api.prefix}/deposit")
@RequiredArgsConstructor
public class DepositController {
    private final DepositService depositService;

    @GetMapping("/config")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<DepositConfigResponse>> getDepositConfig(
            @RequestParam Stablecoin currency,
            @AuthenticationPrincipal UserDetailsImpl currentUser

    ) {
        DepositConfigResponse response =
                depositService.getDepositConfig(currency,currentUser.getId());

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        MessageKeys.DEPOSIT_CONFIG_FETCHED,
                        response
                )
        );
    }

    @PostMapping("/preview")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<DepositPreviewResponse>> previewDeposit(
            @Valid @RequestBody DepositPreviewRequest request
    ) {

        DepositPreviewResponse response =
                depositService.previewDeposit(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        MessageKeys.DEPOSIT_PREVIEW_FETCHED,
                        response
                )
        );
    }

    @PostMapping("/orders")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<CreateDepositOrderResponse>> createDepositOrder(
            @Valid @RequestBody CreateDepositOrderRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {

        CreateDepositOrderResponse response =
                depositService.createDepositOrder(
                        request,
                        currentUser.getId()
                );

        return ResponseEntity.ok(
                ApiResponse.created(
                        HttpStatus.CREATED.value(),
                        MessageKeys.DEPOSIT_ORDER_CREATED,
                        response
                )
        );
    }
}
