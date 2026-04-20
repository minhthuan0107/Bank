package com.example.bank.service.wallet.user;

import com.example.bank.dto.response.wallet.user.CashbackDashboardResponse;

public interface CashbackService {
    CashbackDashboardResponse getDashboard(Long userId);
}
