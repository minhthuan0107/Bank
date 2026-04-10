package com.example.bank.service.wallet.user;

import com.example.bank.dto.response.wallet.user.DashboardResponse;

public interface WalletService {
    DashboardResponse getDashboard(Long userId);

}
