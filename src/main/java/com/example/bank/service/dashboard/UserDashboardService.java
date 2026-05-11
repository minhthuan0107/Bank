package com.example.bank.service.dashboard;

import com.example.bank.dto.response.wallet.user.UserAssetAllocationResponse;
import com.example.bank.dto.response.wallet.user.UserCashFlowResponse;
import com.example.bank.enums.dashboard.DashboardPeriod;

public interface UserDashboardService {

    UserAssetAllocationResponse getAssetAllocation(Long userId);

    UserCashFlowResponse getCashFlow(Long userId, DashboardPeriod period);

}
