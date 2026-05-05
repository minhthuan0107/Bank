package com.example.bank.service.dashboard.impl;

import com.example.bank.dto.response.dashboard.AdminCashFlowResponse;
import com.example.bank.dto.response.dashboard.AdminDashboardSummaryResponse;
import com.example.bank.dto.response.dashboard.CashFlowPointResponse;
import com.example.bank.enums.dashboard.DashboardPeriod;
import com.example.bank.projection.CashFlowProjection;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.repository.wallet.DepositOrderRepository;
import com.example.bank.repository.wallet.WalletRepository;
import com.example.bank.repository.wallet.WithdrawOrderRepository;
import com.example.bank.service.dashboard.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WithdrawOrderRepository withdrawOrderRepository;
    private final DepositOrderRepository depositOrderRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardSummaryResponse getSummary() {
        return AdminDashboardSummaryResponse.builder()
                .totalUsers(userRepository.countNormalUsers())
                .pendingWithdrawCount(withdrawOrderRepository.countPendingAdminWithdraws())
                .pendingWithdrawAmount(withdrawOrderRepository.sumPendingAdminWithdrawAmount())
                .pendingDepositCount(depositOrderRepository.countPendingDeposits())
                .pendingDepositAmount(depositOrderRepository.sumPendingDepositAmount())
                .totalWalletBalance(walletRepository.sumTotalWalletBalance())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminCashFlowResponse getCashFlow(DashboardPeriod period) {
        if (period == null) {
            period = DashboardPeriod.WEEK;
        }

        ZoneId zone = ZoneOffset.UTC;

        Instant start;
        Instant end;

        List<CashFlowProjection> depositRaw;
        List<CashFlowProjection> withdrawRaw;

        switch (period) {
            case WEEK -> {
                LocalDate today = LocalDate.now(zone);
                LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate nextMonday = monday.plusWeeks(1);

                start = monday.atStartOfDay(zone).toInstant();
                end = nextMonday.atStartOfDay(zone).toInstant();

                depositRaw = depositOrderRepository.sumDepositByWeek(start, end);
                withdrawRaw = withdrawOrderRepository.sumWithdrawByWeek(start, end);

                return AdminCashFlowResponse.builder()
                        .period(period)
                        .items(buildWeekItems(depositRaw, withdrawRaw))
                        .build();
            }

            case MONTH -> {
                LocalDate today = LocalDate.now(zone);
                LocalDate firstDay = today.withDayOfMonth(1);
                LocalDate nextMonth = firstDay.plusMonths(1);

                start = firstDay.atStartOfDay(zone).toInstant();
                end = nextMonth.atStartOfDay(zone).toInstant();

                depositRaw = depositOrderRepository.sumDepositByMonth(start, end);
                withdrawRaw = withdrawOrderRepository.sumWithdrawByMonth(start, end);

                return AdminCashFlowResponse.builder()
                        .period(period)
                        .items(buildMonthItems(today.lengthOfMonth(), depositRaw, withdrawRaw))
                        .build();
            }

            case YEAR -> {
                LocalDate today = LocalDate.now(zone);
                LocalDate firstDay = LocalDate.of(today.getYear(), 1, 1);
                LocalDate nextYear = firstDay.plusYears(1);

                start = firstDay.atStartOfDay(zone).toInstant();
                end = nextYear.atStartOfDay(zone).toInstant();

                depositRaw = depositOrderRepository.sumDepositByYear(start, end);
                withdrawRaw = withdrawOrderRepository.sumWithdrawByYear(start, end);

                return AdminCashFlowResponse.builder()
                        .period(period)
                        .items(buildYearItems(depositRaw, withdrawRaw))
                        .build();
            }

            default -> throw new IllegalArgumentException("Unsupported dashboard period");
        }
    }

    private List<CashFlowPointResponse> buildWeekItems(
            List<CashFlowProjection> depositRaw,
            List<CashFlowProjection> withdrawRaw
    ) {
        Map<Integer, BigDecimal> depositMap = toAmountMap(depositRaw);
        Map<Integer, BigDecimal> withdrawMap = toAmountMap(withdrawRaw);

        // MySQL DAYOFWEEK: Sunday=1, Monday=2 ... Saturday=7
        List<Integer> keys = List.of(2, 3, 4, 5, 6, 7, 1);
        List<String> labels = List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");

        List<CashFlowPointResponse> items = new ArrayList<>();

        for (int i = 0; i < keys.size(); i++) {
            Integer key = keys.get(i);

            items.add(CashFlowPointResponse.builder()
                    .label(labels.get(i))
                    .depositAmount(depositMap.getOrDefault(key, BigDecimal.ZERO))
                    .withdrawAmount(withdrawMap.getOrDefault(key, BigDecimal.ZERO))
                    .build());
        }

        return items;
    }

    private List<CashFlowPointResponse> buildMonthItems(
            int daysInMonth,
            List<CashFlowProjection> depositRaw,
            List<CashFlowProjection> withdrawRaw
    ) {
        Map<Integer, BigDecimal> depositMap = toAmountMap(depositRaw);
        Map<Integer, BigDecimal> withdrawMap = toAmountMap(withdrawRaw);

        List<CashFlowPointResponse> items = new ArrayList<>();

        for (int day = 1; day <= daysInMonth; day++) {
            items.add(CashFlowPointResponse.builder()
                    .label(String.format("%02d", day))
                    .depositAmount(depositMap.getOrDefault(day, BigDecimal.ZERO))
                    .withdrawAmount(withdrawMap.getOrDefault(day, BigDecimal.ZERO))
                    .build());
        }

        return items;
    }

    private List<CashFlowPointResponse> buildYearItems(
            List<CashFlowProjection> depositRaw,
            List<CashFlowProjection> withdrawRaw
    ) {
        Map<Integer, BigDecimal> depositMap = toAmountMap(depositRaw);
        Map<Integer, BigDecimal> withdrawMap = toAmountMap(withdrawRaw);

        List<String> labels = List.of(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        );

        List<CashFlowPointResponse> items = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            items.add(CashFlowPointResponse.builder()
                    .label(labels.get(month - 1))
                    .depositAmount(depositMap.getOrDefault(month, BigDecimal.ZERO))
                    .withdrawAmount(withdrawMap.getOrDefault(month, BigDecimal.ZERO))
                    .build());
        }

        return items;
    }

    private Map<Integer, BigDecimal> toAmountMap(List<CashFlowProjection> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }

        return raw.stream()
                .filter(item -> item.getGroupKey() != null)
                .collect(Collectors.toMap(
                        CashFlowProjection::getGroupKey,
                        item -> item.getAmount() == null
                                ? BigDecimal.ZERO
                                : item.getAmount(),
                        BigDecimal::add
                ));
    }
}
