package com.example.bank.service.cashback.user;

import com.example.bank.dto.response.cashback.user.CashbackHistoryPageResponse;
import com.example.bank.dto.response.wallet.user.CashbackDashboardResponse;

public interface CashbackService {
    CashbackDashboardResponse getDashboard(Long userId);

    CashbackHistoryPageResponse getUserCashbackHistory(
            Long userId,
            int page
    );
}
