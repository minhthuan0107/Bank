package com.example.bank.controller.user.wallet;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.request.wallet.user.CreateDepositOrderRequest;
import com.example.bank.dto.request.wallet.user.DepositPreviewRequest;
import com.example.bank.dto.response.wallet.user.*;
import com.example.bank.enums.wallet.DepositOrderStatus;
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

import java.time.Instant;
import java.util.List;


@RestController
@RequestMapping("${api.prefix}/deposit")
@RequiredArgsConstructor
public class DepositController {
    private final DepositService depositService;
    private final LocalizationUtils i18n;

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
                        i18n.getLocalizedMessage(MessageKeys.DEPOSIT_CONFIG_FETCHED),
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
                        i18n.getLocalizedMessage(MessageKeys.DEPOSIT_PREVIEW_FETCHED),
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
                        i18n.getLocalizedMessage(MessageKeys.DEPOSIT_ORDER_CREATED),
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
                        i18n.getLocalizedMessage(MessageKeys.DEPOSIT_PROOF_UPLOADED_SUCCESS)
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
                        i18n.getLocalizedMessage(MessageKeys.DEPOSIT_ORDER_LIST_SUCCESS),
                        data
                )
        );
    }

    @GetMapping("/orders")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<DepositOrderPageResponse>> getDepositOrders(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) DepositOrderStatus status,
            @RequestParam(required = false) Instant fromTime,
            @RequestParam(required = false) Instant toTime,

            @RequestParam(defaultValue = "0") int page
    ) {
        DepositOrderPageResponse response =
                depositService.getDepositOrders(
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
                        i18n.getLocalizedMessage(MessageKeys.DEPOSIT_ORDER_LIST_SUCCESS),
                        response
                )
        );
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<DepositDashboardResponse>> getDepositDashboard(
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        DepositDashboardResponse response =
                depositService.getDepositDashboard(currentUser.getId());

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.DEPOSIT_DASHBOARD_SUCCESS),
                        response
                )
        );
    }
}

