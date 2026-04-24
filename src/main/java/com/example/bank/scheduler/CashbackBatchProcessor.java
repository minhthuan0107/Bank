package com.example.bank.scheduler;

import com.example.bank.entity.wallet.CashbackRule;
import com.example.bank.entity.wallet.UserCashbackMonthly;
import com.example.bank.enums.wallet.CashbackStatus;
import com.example.bank.repository.UserSpentProjection;
import com.example.bank.repository.wallet.UserCashbackMonthlyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashbackBatchProcessor {

    private final UserCashbackMonthlyRepository monthlyRepository;

    @Transactional
    public void processBatch(
            List<UserSpentProjection> batch,
            List<CashbackRule> rules,
            String month
    ) {
        if (batch == null || batch.isEmpty()) {
            return;
        }

        List<Long> userIds = batch.stream()
                .map(UserSpentProjection::getUserId)
                .toList();

        Map<Long, UserCashbackMonthly> existing =
                monthlyRepository.findByUserIdInAndMonth(userIds, month)
                        .stream()
                        .collect(Collectors.toMap(
                                UserCashbackMonthly::getUserId,
                                m -> m
                        ));

        List<UserCashbackMonthly> toSave = new ArrayList<>();

        for (UserSpentProjection p : batch) {
            Long userId = p.getUserId();
            BigDecimal totalSpent = p.getTotalSpent();

            if (userId == null || totalSpent == null || totalSpent.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            CashbackRule rule = findMatchedRule(rules, totalSpent);
            if (rule == null) {
                continue;
            }

            BigDecimal cashback = totalSpent
                    .multiply(rule.getCashbackPercent())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

            UserCashbackMonthly monthly = existing.get(userId);

            // Đã chốt rồi thì không được scheduler ghi đè nữa
            if (monthly != null &&
                    (monthly.getStatus() == CashbackStatus.APPROVED
                            || monthly.getStatus() == CashbackStatus.REJECTED)) {
                continue;
            }

            if (monthly == null) {
                monthly = new UserCashbackMonthly();
                monthly.setUserId(userId);
                monthly.setMonth(month);
                monthly.setStatus(CashbackStatus.PENDING);
            }

            // overwrite để job idempotent, chạy lại không bị cộng dồn
            monthly.setTotalSpent(totalSpent);
            monthly.setCashbackAmount(cashback);
            monthly.setPercent(rule.getCashbackPercent());

            toSave.add(monthly);
        }

        if (!toSave.isEmpty()) {
            monthlyRepository.saveAll(toSave);
        }

        log.info("Processed cashback batch month={} batchSize={} saved={}",
                month, batch.size(), toSave.size());
    }

    private CashbackRule findMatchedRule(
            List<CashbackRule> rules,
            BigDecimal totalSpent
    ) {
        if (rules == null || rules.isEmpty() || totalSpent == null) {
            return null;
        }

        for (CashbackRule rule : rules) {
            boolean meetsMin = totalSpent.compareTo(rule.getMinSpent()) >= 0;

            boolean meetsMax = rule.getMaxSpent() == null
                    || totalSpent.compareTo(rule.getMaxSpent()) < 0;

            if (meetsMin && meetsMax) {
                return rule;
            }
        }

        return null;
    }
}