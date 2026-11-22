package com.example.bank.service.wallet.user;

import com.example.bank.dto.response.wallet.user.BalanceResponse;
import com.example.bank.dto.response.wallet.user.DashboardResponse;

public interface WalletService {
    DashboardResponse getDashboard(Long userId);

    boolean isWalletQualified(Long userId);

    /** Wallet available balance plus the balance of all cards (Slash and Infini). */
    BalanceResponse getUserBalance(Long userId);

}
