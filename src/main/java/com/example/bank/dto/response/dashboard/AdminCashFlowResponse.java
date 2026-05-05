package com.example.bank.dto.response.dashboard;

import com.example.bank.enums.dashboard.DashboardPeriod;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminCashFlowResponse {

    private DashboardPeriod period;

    private List<CashFlowPointResponse> items;
}