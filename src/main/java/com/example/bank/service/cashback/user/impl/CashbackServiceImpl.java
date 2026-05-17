package com.example.bank.service.cashback.user.impl;

import com.example.bank.common.config.properties.WalletProperties;
import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.response.cashback.user.CashbackHistoryPageResponse;
import com.example.bank.dto.response.cashback.user.CashbackHistoryResponse;
import com.example.bank.dto.response.wallet.user.CashbackDashboardResponse;
import com.example.bank.dto.response.wallet.user.CashbackTierResponse;
import com.example.bank.entity.wallet.CashbackRule;
import com.example.bank.entity.wallet.UserCashbackMonthly;
import com.example.bank.enums.wallet.CashbackStatus;
import com.example.bank.repository.wallet.CardTransactionRepository;
import com.example.bank.repository.wallet.CashbackRuleRepository;
import com.example.bank.repository.wallet.UserCashbackMonthlyRepository;
import com.example.bank.service.cashback.user.CashbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CashbackServiceImpl implements CashbackService {

    private final CardTransactionRepository cardTransactionRepository;
    private final CashbackRuleRepository cashbackRuleRepository;
    private final UserCashbackMonthlyRepository monthlyRepository;
    private final WalletProperties walletProperties;
    private final UserCashbackMonthlyRepository userCashbackMonthlyRepository;


    @Override
    @Transactional(readOnly = true)
    public CashbackDashboardResponse getDashboard(Long userId) {
        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
        String month = currentMonth.toString();

        /*
         * Ưu tiên đọc snapshot từ bảng user_cashback_monthly.
         *
         * Lý do:
         * - Admin duyệt cashback dựa trên bảng này.
         * - User dashboard cũng nên hiển thị theo bảng này.
         * - Tránh lệch số giữa user và admin khi scheduler chỉ chạy mỗi 3 tiếng.
         */
        UserCashbackMonthly monthly = monthlyRepository
                .findByUserIdAndMonth(userId, month)
                .orElse(null);

        if (monthly != null) {
            return buildSnapshotDashboard(monthly);
        }

        /*
         * Chỉ fallback realtime khi scheduler chưa tạo snapshot tháng hiện tại.
         * Ví dụ: đầu tháng, server mới deploy, hoặc job chưa chạy kịp.
         */
        return buildRealtimePreview(userId, currentMonth);
    }

    /**
     * Build dashboard từ snapshot user_cashback_monthly.
     * Dùng cho cả PENDING / APPROVED / REJECTED.
     */
    private CashbackDashboardResponse buildSnapshotDashboard(UserCashbackMonthly monthly) {
        List<CashbackRule> rules = cashbackRuleRepository
                .findAllByIsActiveTrueOrderByMinSpentAsc();

        BigDecimal totalSpent = defaultZero(monthly.getTotalSpent());
        BigDecimal cashbackAmount = defaultZero(monthly.getCashbackAmount());
        BigDecimal currentPercent = monthly.getPercent();

        List<CashbackTierResponse> tiers = buildTiers(
                rules,
                totalSpent,
                shouldHighlightTier(monthly.getStatus())
        );

        return CashbackDashboardResponse.builder()
                .totalSpent(totalSpent)
                .cashbackAmount(cashbackAmount)
                .currentPercent(currentPercent)
                .tiers(tiers)
                .build();
    }

    /**
     * Preview realtime khi chưa có snapshot.
     * Không ghi DB ở đây để tránh dashboard tự tạo dữ liệu lệch flow scheduler/admin.
     */
    private CashbackDashboardResponse buildRealtimePreview(Long userId, YearMonth currentMonth) {
        Instant start = currentMonth
                .atDay(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        Instant end = currentMonth
                .plusMonths(1)
                .atDay(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        BigDecimal totalSpent = cardTransactionRepository
                .sumByUserAndMonth(userId, start, end);

        totalSpent = defaultZero(totalSpent);

        List<CashbackRule> rules = cashbackRuleRepository
                .findAllByIsActiveTrueOrderByMinSpentAsc();

        CashbackRule matchedRule = findMatchedRule(rules, totalSpent);

        BigDecimal cashbackAmount = BigDecimal.ZERO;
        BigDecimal currentPercent = BigDecimal.ZERO;

        if (matchedRule != null) {
            currentPercent = matchedRule.getCashbackPercent();

            cashbackAmount = totalSpent
                    .multiply(matchedRule.getCashbackPercent())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        }

        List<CashbackTierResponse> tiers = buildTiers(
                rules,
                totalSpent,
                true
        );

        return CashbackDashboardResponse.builder()
                .totalSpent(totalSpent)
                .cashbackAmount(cashbackAmount)
                .currentPercent(currentPercent)
                .tiers(tiers)
                .build();
    }

    /**
     * Build danh sách tier cho FE hiển thị progress/highlight.
     */
    private List<CashbackTierResponse> buildTiers(
            List<CashbackRule> rules,
            BigDecimal totalSpent,
            boolean highlightCurrentTier
    ) {
        BigDecimal spent = defaultZero(totalSpent);

        return rules.stream()
                .map(rule -> {
                    boolean isCurrent = highlightCurrentTier && isMatched(rule, spent);

                    return CashbackTierResponse.builder()
                            .minSpent(rule.getMinSpent())
                            .maxSpent(rule.getMaxSpent())
                            .percent(rule.getCashbackPercent())
                            .isCurrent(isCurrent)
                            .build();
                })
                .toList();
    }

    /**
     * Match rule theo dạng:
     *
     * min_spent <= totalSpent < max_spent
     *
     * Nếu max_spent = null thì hiểu là không giới hạn trên.
     */
    private CashbackRule findMatchedRule(List<CashbackRule> rules, BigDecimal totalSpent) {
        if (rules == null || rules.isEmpty() || totalSpent == null) {
            return null;
        }

        return rules.stream()
                .filter(rule -> isMatched(rule, totalSpent))
                .max(Comparator.comparing(CashbackRule::getMinSpent))
                .orElse(null);
    }

    private boolean isMatched(CashbackRule rule, BigDecimal totalSpent) {
        if (rule == null || totalSpent == null || rule.getMinSpent() == null) {
            return false;
        }

        boolean meetsMin = totalSpent.compareTo(rule.getMinSpent()) >= 0;

        boolean meetsMax = rule.getMaxSpent() == null
                || totalSpent.compareTo(rule.getMaxSpent()) < 0;

        return meetsMin && meetsMax;
    }

    /**
     * APPROVED/REJECTED là trạng thái đã chốt, không cần highlight tier hiện tại nữa.
     * PENDING vẫn highlight để user thấy đang thuộc tier nào.
     */
    private boolean shouldHighlightTier(CashbackStatus status) {
        return status == null || status == CashbackStatus.PENDING;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
    @Override
    public CashbackHistoryPageResponse getUserCashbackHistory(
            Long userId,
            int page
    ) {
        int size = walletProperties.getDefaultPageSize();

        // ===== 2. Validate page =====
        if (page < 0) {
            page = 0;
        }

        // ===== 4. Query DB: chỉ lấy APPROVED =====
        Page<UserCashbackMonthly> pageData =
                userCashbackMonthlyRepository.findByUserIdAndStatus(
                        userId,
                        CashbackStatus.APPROVED,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(Sort.Direction.DESC, "month")
                                        .and(Sort.by(Sort.Direction.DESC, "id"))
                        )
                );

        // ===== 5. Map response =====
        List<CashbackHistoryResponse> items = pageData.getContent()
                .stream()
                .map(m -> CashbackHistoryResponse.builder()
                        .userId(m.getUserId())
                        .month(m.getMonth())
                        .totalSpent(m.getTotalSpent())
                        .cashbackAmount(m.getCashbackAmount())
                        .percent(m.getPercent())
                        .cashbackStatus(m.getStatus())
                        .approvedAt(m.getApprovedAt())
                        .build()
                )
                .toList();

        // ===== 6. Return page response =====
        return CashbackHistoryPageResponse.builder()
                .items(items)
                .page(page)
                .size(size)
                .totalSize(pageData.getTotalElements())
                .hasNext(pageData.hasNext())
                .build();
    }

}