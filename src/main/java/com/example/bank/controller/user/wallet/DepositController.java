package com.example.bank.controller.user.wallet;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.dto.request.wallet.user.CreateDepositOrderRequest;
import com.example.bank.dto.request.wallet.user.DepositPreviewRequest;
import com.example.bank.dto.response.wallet.user.CreateDepositOrderResponse;
import com.example.bank.dto.response.wallet.user.DepositConfigResponse;
import com.example.bank.dto.response.wallet.user.DepositOrderPageResponse;
import com.example.bank.dto.response.wallet.user.DepositPreviewResponse;
import com.example.bank.enums.wallet.Stablecoin;
import com.example.bank.service.wallet.user.DepositService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


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

    @PostMapping(
            value = "/orders/{orderNo}/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<?>> uploadDepositImages(

            @PathVariable String orderNo,
            @RequestPart("images")
            List<MultipartFile> images,
            @AuthenticationPrincipal UserDetailsImpl currentUser

    ) {

        depositService.uploadDepositImages(orderNo, images,currentUser.getId());

        return ResponseEntity.ok(
                ApiResponse.ok(
                        HttpStatus.OK.value(),
                        MessageKeys.DEPOSIT_PROOF_UPLOADED_SUCCESS
                )
        );
    }

    @GetMapping("/orders-list")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<DepositOrderPageResponse>> getDepositOrders(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @AuthenticationPrincipal UserDetailsImpl currentUser

    ) {
        DepositOrderPageResponse data =
                depositService.getUserDepositOrders(
                        currentUser.getId(),
                        page
                );
        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        MessageKeys.DEPOSIT_ORDER_LIST_SUCCESS,
                        data
                )
        );
    }
}

