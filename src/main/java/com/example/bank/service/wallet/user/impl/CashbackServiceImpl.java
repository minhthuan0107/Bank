package com.example.bank.service.wallet.user.impl;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.response.wallet.user.CashbackDashboardResponse;
import com.example.bank.dto.response.wallet.user.CashbackTierResponse;
import com.example.bank.entity.wallet.CashbackRule;
import com.example.bank.entity.wallet.UserCashbackMonthly;
import com.example.bank.enums.wallet.CashbackStatus;
import com.example.bank.repository.wallet.CardTransactionRepository;
import com.example.bank.repository.wallet.CashbackRuleRepository;
import com.example.bank.repository.wallet.UserCashbackMonthlyRepository;
import com.example.bank.service.wallet.user.CashbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CashbackServiceImpl implements CashbackService {

    private final CardTransactionRepository cardTransactionRepository;
    private final CashbackRuleRepository cashbackRuleRepository;
    private final UserCashbackMonthlyRepository monthlyRepository;


    public CashbackDashboardResponse getDashboard(Long userId) {
        String month = YearMonth.now().toString();
        // ===== Lấy record tháng (KHÔNG tạo mới) =====
        UserCashbackMonthly monthly = monthlyRepository
                .findByUserIdAndMonth(userId, month)
                .orElse(null);

        // ===== Nếu đã APPROVED → trả luôn (freeze) =====
        if (monthly != null && monthly.getStatus() == CashbackStatus.APPROVED) {
            return buildApprovedResponse(monthly);
        }

        // ===== Time range tháng =====
        Instant start = YearMonth.now()
                .atDay(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        Instant end = YearMonth.now()
                .plusMonths(1)
                .atDay(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        // ===== SUM spent (PENDING + POSTED) =====
        BigDecimal totalSpent = cardTransactionRepository
                .sumByUserAndMonth(userId, start, end);

        if (totalSpent == null) totalSpent = BigDecimal.ZERO;

        // ===== Tìm rule =====
        CashbackRule rule = cashbackRuleRepository
                .findMatchedRule(totalSpent)
                .orElseThrow(() -> new WalletException(
                        MessageKeys.CASHBACK_RULE_NOT_FOUND,
                        HttpStatus.NOT_FOUND
                ));

        // ===== Tính cashback =====
        BigDecimal cashback = totalSpent
                .multiply(rule.getCashbackPercent())
                .divide(BigDecimal.valueOf(100));

        // ===== Build tiers để FE highlight =====
        List<CashbackRule> rules = cashbackRuleRepository
                .findAllByIsActiveTrueOrderByMinSpentAsc();

        BigDecimal spent = totalSpent; // đảm bảo effectively final

        List<CashbackTierResponse> tiers = rules.stream()
                .map(r -> {
                    boolean isCurrent = spent.compareTo(r.getMinSpent()) >= 0 &&
                            (r.getMaxSpent() == null ||
                                    spent.compareTo(r.getMaxSpent()) < 0);

                    return CashbackTierResponse.builder()
                            .minSpent(r.getMinSpent())
                            .maxSpent(r.getMaxSpent())
                            .percent(r.getCashbackPercent())
                            .isCurrent(isCurrent)
                            .build();
                })
                .toList();

        return CashbackDashboardResponse.builder()
                .totalSpent(totalSpent)
                .cashbackAmount(cashback)
                .currentPercent(rule.getCashbackPercent())
                .tiers(tiers)
                .build();
    }

    // ===== response khi đã approve =====
    private CashbackDashboardResponse buildApprovedResponse(UserCashbackMonthly monthly) {
        List<CashbackRule> rules = cashbackRuleRepository
                .findAllByIsActiveTrueOrderByMinSpentAsc();

        List<CashbackTierResponse> tiers = rules.stream()
                .map(r -> CashbackTierResponse.builder()
                        .minSpent(r.getMinSpent())
                        .maxSpent(r.getMaxSpent())
                        .percent(r.getCashbackPercent())
                        .isCurrent(false) // đã chốt → không cần highlight
                        .build()
                )
                .toList();

        return CashbackDashboardResponse.builder()
                .totalSpent(monthly.getTotalSpent())
                .cashbackAmount(monthly.getCashbackAmount())
                .currentPercent(null)
                .tiers(tiers)
                .build();
    }

}