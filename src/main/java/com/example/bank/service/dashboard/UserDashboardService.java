package com.example.bank.service.dashboard;

import com.example.bank.dto.response.wallet.user.UserAssetAllocationResponse;

public interface UserDashboardService {

    UserAssetAllocationResponse getAssetAllocation(Long userId);

}
