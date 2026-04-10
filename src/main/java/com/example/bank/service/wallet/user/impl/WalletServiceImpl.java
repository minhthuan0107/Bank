package com.example.bank.service.wallet.user.impl;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.response.wallet.user.DashboardResponse;
import com.example.bank.entity.user.User;
import com.example.bank.repository.projection.WalletSummaryProjection;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.repository.wallet.CardRepository;
import com.example.bank.repository.wallet.WalletRepository;
import com.example.bank.service.wallet.user.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {
    private final WalletRepository walletRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    @Override
    public DashboardResponse getDashboard(Long userId) {
        WalletSummaryProjection wallet = walletRepository.getWalletSummary(userId);
        Long cardCount = cardRepository.getActivatedCardCount(userId);
        if (cardCount == null) cardCount = 0L;
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.USER_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));
        Integer cardLimit = user.getCardOpenLimit();
        return DashboardResponse.builder()
                .userId(userId)
                .totalBalance(wallet != null && wallet.getTotalBalance() != null ? wallet.getTotalBalance() : BigDecimal.ZERO)
                .availableBalance(wallet != null && wallet.getAvailableBalance() != null ? wallet.getAvailableBalance() : BigDecimal.ZERO)
                .frozenBalance(wallet != null && wallet.getFrozenBalance() != null ? wallet.getFrozenBalance() : BigDecimal.ZERO)
                .allocatedBalance(wallet != null && wallet.getAllocatedBalance() != null ? wallet.getAllocatedBalance() : BigDecimal.ZERO)
                .cardOpeningLimit(cardLimit)
                .activatedCardCount(cardCount)
                .build();
    }
}
