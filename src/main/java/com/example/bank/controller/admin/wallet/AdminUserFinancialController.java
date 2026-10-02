package com.example.bank.controller.admin.wallet;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.response.dashboard.AdminUserFinancialStatisticsResponse;
import com.example.bank.dto.response.wallet.admin.DepositOrderPageAdminResponse;
import com.example.bank.enums.wallet.DepositOrderStatus;
import com.example.bank.service.wallet.admin.AdminUserFinancialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/admin/users")
public class AdminUserFinancialController {

    private final AdminUserFinancialService service;
    private final LocalizationUtils i18n;

    @GetMapping("/{userId}/financial-statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminUserFinancialStatisticsResponse>> getFinancialStatistics(
            @PathVariable Long userId
    ) {
        AdminUserFinancialStatisticsResponse response =
                service.getFinancialStatistics(userId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(
                                MessageKeys.ADMIN_USER_FINANCIAL_STATISTICS_SUCCESS
                        ),
                        response
                )
        );
    }



}