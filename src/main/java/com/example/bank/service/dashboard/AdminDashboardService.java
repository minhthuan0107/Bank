package com.example.bank.service.dashboard;

import com.example.bank.dto.response.dashboard.AdminCashFlowResponse;
import com.example.bank.dto.response.dashboard.AdminDashboardSummaryResponse;
import com.example.bank.enums.dashboard.DashboardPeriod;

public interface AdminDashboardService {

    AdminDashboardSummaryResponse getSummary();

    AdminCashFlowResponse getCashFlow(DashboardPeriod period);
}