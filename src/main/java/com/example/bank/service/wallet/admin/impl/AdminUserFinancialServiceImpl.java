package com.example.bank.service.wallet.admin.impl;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.response.dashboard.AdminCardTransactionStatisticsResponse;
import com.example.bank.dto.response.dashboard.AdminDepositStatisticsResponse;
import com.example.bank.dto.response.dashboard.AdminUserFinancialStatisticsResponse;
import com.example.bank.dto.response.dashboard.AdminWithdrawStatisticsResponse;
import com.example.bank.repository.projection.AdminCardTransactionStatisticsProjection;
import com.example.bank.repository.projection.AdminDepositStatisticsProjection;
import com.example.bank.repository.projection.AdminWithdrawStatisticsProjection;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.repository.wallet.CardTransactionRepository;
import com.example.bank.repository.wallet.DepositOrderRepository;
import com.example.bank.repository.wallet.WithdrawOrderRepository;
import com.example.bank.service.wallet.admin.AdminUserFinancialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class AdminUserFinancialServiceImpl implements AdminUserFinancialService {

    private final UserRepository userRepository;
    private final DepositOrderRepository depositOrderRepository;
    private final WithdrawOrderRepository withdrawOrderRepository;
    private final CardTransactionRepository cardTransactionRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminUserFinancialStatisticsResponse getFinancialStatistics(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new WalletException(
                    MessageKeys.USER_NOT_FOUND,
                    HttpStatus.NOT_FOUND
            );
        }

        AdminDepositStatisticsProjection deposit =
                depositOrderRepository.getAdminUserStatistics(userId);

        AdminWithdrawStatisticsProjection withdraw =
                withdrawOrderRepository.getAdminUserStatistics(userId);

        AdminCardTransactionStatisticsProjection card =
                cardTransactionRepository.getAdminUserStatistics(userId);

        return AdminUserFinancialStatisticsResponse.builder()
                .deposit(toDepositResponse(deposit))
                .withdraw(toWithdrawResponse(withdraw))
                .cardTransaction(toCardResponse(card))
                .build();
    }

    private AdminDepositStatisticsResponse toDepositResponse(
            AdminDepositStatisticsProjection p
    ) {
        long successCount = safe(p.getSuccessCount());
        long failedCount = safe(p.getFailedCount());

        return AdminDepositStatisticsResponse.builder()
                .successCount(successCount)
                .successAmount(safe(p.getSuccessAmount()))
                .pendingCount(safe(p.getPendingCount()))
                .pendingAmount(safe(p.getPendingAmount()))
                .failedCount(failedCount)
                .failedAmount(safe(p.getFailedAmount()))
                .successRate(rate(successCount, successCount + failedCount))
                .build();
    }

    private AdminWithdrawStatisticsResponse toWithdrawResponse(
            AdminWithdrawStatisticsProjection p
    ) {
        long successCount = safe(p.getSuccessCount());
        long failedCount = safe(p.getFailedCount());

        return AdminWithdrawStatisticsResponse.builder()
                .successCount(successCount)
                .successAmount(safe(p.getSuccessAmount()))
                .pendingCount(safe(p.getPendingCount()))
                .pendingAmount(safe(p.getPendingAmount()))
                .failedCount(failedCount)
                .failedAmount(safe(p.getFailedAmount()))
                .successRate(rate(successCount, successCount + failedCount))
                .build();
    }

    private AdminCardTransactionStatisticsResponse toCardResponse(
            AdminCardTransactionStatisticsProjection p
    ) {
        long postedCount = safe(p.getPostedCount());
        long pendingCount = safe(p.getPendingCount());
        long failedCount = safe(p.getFailedCount());
        long reversedCount = safe(p.getReversedCount());

        BigDecimal postedAmount = safe(p.getPostedAmount());
        BigDecimal pendingAmount = safe(p.getPendingAmount());

        long successCount = postedCount + pendingCount;
        BigDecimal successAmount = postedAmount.add(pendingAmount);

        long transactionTotal =
                successCount + failedCount + reversedCount;

        long deductedTotal =
                postedCount + pendingCount + reversedCount;

        return AdminCardTransactionStatisticsResponse.builder()
                .successCount(successCount)
                .successAmount(successAmount)
                .postedCount(postedCount)
                .postedAmount(postedAmount)
                .pendingCount(pendingCount)
                .pendingAmount(pendingAmount)
                .failedCount(failedCount)
                .failedAmount(safe(p.getFailedAmount()))
                .reversedCount(reversedCount)
                .reversedAmount(safe(p.getReversedAmount()))
                .successRate(rate(successCount, transactionTotal))
                .refundRate(rate(reversedCount, deductedTotal))
                .build();
    }

    private BigDecimal rate(long value, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(value)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private long safe(Long value) {
        return value != null ? value : 0L;
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

}
