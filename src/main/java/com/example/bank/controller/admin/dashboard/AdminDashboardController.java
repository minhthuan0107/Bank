package com.example.bank.controller.admin.dashboard;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.response.ApiResponse;
import com.example.bank.common.utils.LocalizationUtils;
import com.example.bank.dto.response.dashboard.AdminCashFlowResponse;
import com.example.bank.dto.response.dashboard.AdminDashboardSummaryResponse;
import com.example.bank.enums.dashboard.DashboardPeriod;
import com.example.bank.service.dashboard.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("${api.prefix}/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;
    private final LocalizationUtils i18n;

    /**
     * Lấy thống kê tổng quan dashboard admin
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminDashboardSummaryResponse>> getSummary() {
        AdminDashboardSummaryResponse response =
                dashboardService.getSummary();

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.ADMIN_DASHBOARD_SUMMARY_SUCCESS),
                        response
                )
        );
    }

    /**
     * Lấy dữ liệu biểu đồ Deposit vs Withdraw theo WEEK / MONTH / YEAR
     */
    @GetMapping("/cash-flow")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminCashFlowResponse>> getCashFlow(
            @RequestParam(defaultValue = "WEEK") DashboardPeriod period
    ) {
        AdminCashFlowResponse response =
                dashboardService.getCashFlow(period);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        i18n.getLocalizedMessage(MessageKeys.ADMIN_DASHBOARD_CASH_FLOW_SUCCESS),
                        response
                )
        );
    }
}
