package com.example.bank.service.cashback.admin.impl;

import com.example.bank.entity.wallet.CashbackRule;
import com.example.bank.projection.UserSpentProjection;
import com.example.bank.repository.wallet.CardTransactionRepository;
import com.example.bank.repository.wallet.CashbackRuleRepository;
import com.example.bank.scheduler.CashbackBatchProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashbackBatchService {

    private final CardTransactionRepository cardTransactionRepository;
    private final CashbackRuleRepository ruleRepository;
    private final CashbackBatchProcessor batchProcessor;

    private static final int BATCH_SIZE = 200;

    public void generateMonthlyCashback() {

        YearMonth ym = YearMonth.now(ZoneOffset.UTC);
        String month = ym.toString();

        Instant start = ym.atDay(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        Instant end = ym.plusMonths(1)
                .atDay(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();

        // ===== SUM tất cả user =====
        List<UserSpentProjection> all =
                cardTransactionRepository.sumAllUsers(start, end);

        if (all.isEmpty()) {
            log.info("No cashback transaction data found month={}", month);
            return;
        }

        // ===== Load rules =====
        List<CashbackRule> rules =
                ruleRepository.findAllByIsActiveTrueOrderByMinSpentAsc();

        if (rules.isEmpty()) {
            log.warn("No active cashback rules found. Skip cashback batch month={}", month);
            return;
        }

        // ===== Chia batch =====
        for (int i = 0; i < all.size(); i += BATCH_SIZE) {
            List<UserSpentProjection> batch =
                    all.subList(i, Math.min(i + BATCH_SIZE, all.size()));

            batchProcessor.processBatch(batch, rules, month);
        }

        log.info("End cashback monthly batch month={} totalUsers={}", month, all.size());
    }
}