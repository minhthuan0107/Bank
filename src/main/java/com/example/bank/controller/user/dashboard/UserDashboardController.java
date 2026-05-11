package com.example.bank.controller.user.dashboard;

import com.example.bank.common.config.security.UserDetailsImpl;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.response.wallet.user.UserAssetAllocationResponse;
import com.example.bank.dto.response.wallet.user.UserCashFlowResponse;
import com.example.bank.enums.dashboard.DashboardPeriod;
import com.example.bank.service.dashboard.UserDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.prefix}/dashboard")
@RequiredArgsConstructor
public class UserDashboardController {

    private final UserDashboardService userDashboardService;
    private final LocalizationUtils i18n;

    @GetMapping("/asset-allocation")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<UserAssetAllocationResponse>> getAssetAllocation(
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        UserAssetAllocationResponse response =
                userDashboardService.getAssetAllocation(currentUser.getId());

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.USER_ASSET_ALLOCATION_SUCCESS),
                        response
                )
        );
    }

    @GetMapping("/cash-flow")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<UserCashFlowResponse>> getUserCashFlow(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @RequestParam(defaultValue = "WEEK") DashboardPeriod period
    ) {
        UserCashFlowResponse response =
                userDashboardService.getCashFlow(currentUser.getId(), period);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.USER_DASHBOARD_CASH_FLOW_SUCCESS),
                        response
                )
        );
    }
}
