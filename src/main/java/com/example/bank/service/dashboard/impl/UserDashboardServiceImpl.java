package com.example.bank.service.dashboard.impl;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.response.wallet.user.UserAssetAllocationItemResponse;
import com.example.bank.dto.response.wallet.user.UserAssetAllocationResponse;
import com.example.bank.dto.response.wallet.user.UserCashFlowPointResponse;
import com.example.bank.dto.response.wallet.user.UserCashFlowResponse;
import com.example.bank.enums.dashboard.AssetAllocationKey;
import com.example.bank.enums.dashboard.DashboardPeriod;
import com.example.bank.projection.CashFlowProjection;
import com.example.bank.projection.WalletAssetAllocationProjection;
import com.example.bank.repository.wallet.DepositOrderRepository;
import com.example.bank.repository.wallet.WalletRepository;
import com.example.bank.repository.wallet.WithdrawOrderRepository;
import com.example.bank.service.dashboard.UserDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDashboardServiceImpl implements UserDashboardService {

    private final WalletRepository walletRepository;
    private final DepositOrderRepository depositOrderRepository;
    private final WithdrawOrderRepository withdrawOrderRepository;


    @Override
    @Transactional(readOnly = true)
    public UserAssetAllocationResponse getAssetAllocation(Long userId) {
        /*
         * Lấy snapshot tài sản hiện tại của user từ bảng wallets.
         * Không query cards riêng vì allocated_balance đã là tổng tiền đang phân bổ vào thẻ.
         */
        WalletAssetAllocationProjection wallet =
                walletRepository.findAssetAllocationByUserId(userId)
                        .orElseThrow(() -> new WalletException(
                                MessageKeys.WALLET_NOT_FOUND,
                                HttpStatus.NOT_FOUND
                        ));
        /*
         * total_balance là tổng tài sản thật của ví.
         * Theo invariant:
         * total_balance = available_balance + allocated_balance + frozen_balance
         */
        BigDecimal totalAmount = safe(wallet.getTotalBalance());
        BigDecimal availableBalance = safe(wallet.getAvailableBalance());
        BigDecimal allocatedBalance = safe(wallet.getAllocatedBalance());
        BigDecimal frozenBalance = safe(wallet.getFrozenBalance());

        return UserAssetAllocationResponse.builder()
                .totalAmount(totalAmount)
                .items(List.of(
                        buildAssetItem(
                                AssetAllocationKey.AVAILABLE_BALANCE,
                                availableBalance,
                                totalAmount
                        ),
                        buildAssetItem(
                                AssetAllocationKey.ALLOCATED_BALANCE,
                                allocatedBalance,
                                totalAmount
                        ),
                        buildAssetItem(
                                AssetAllocationKey.FROZEN_BALANCE,
                                frozenBalance,
                                totalAmount
                        )
                ))
                .build();
    }
    /*
     * Build từng phần của biểu đồ tròn.
     * amount giữ số tiền thật để FE có thể hiển thị tooltip,
     * percent dùng để vẽ tỷ lệ trên biểu đồ.
     */
    private UserAssetAllocationItemResponse buildAssetItem(
            AssetAllocationKey key,
            BigDecimal amount,
            BigDecimal totalAmount
    ) {
        BigDecimal safeAmount = safe(amount);

        return UserAssetAllocationItemResponse.builder()
                .key(key.getValue())
                .amount(safeAmount)
                .percent(calculatePercent(safeAmount, totalAmount))
                .build();
    }

    private BigDecimal calculatePercent(BigDecimal amount, BigDecimal totalAmount) {
        /*
         * Tính tỷ lệ phần trăm trên tổng tài sản.
         * Nếu totalAmount <= 0 thì trả 0.00 để tránh chia cho 0.
         */
        if (amount == null || totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return amount
                .multiply(BigDecimal.valueOf(100))
                .divide(totalAmount, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    @Override
    @Transactional(readOnly = true)
    public UserCashFlowResponse getCashFlow(Long userId, DashboardPeriod period) {
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

                depositRaw = depositOrderRepository.sumUserDepositByWeek(userId, start, end);
                withdrawRaw = withdrawOrderRepository.sumUserWithdrawByWeek(userId, start, end);

                return UserCashFlowResponse.builder()
                        .period(period)
                        .items(buildUserWeekItems(depositRaw, withdrawRaw))
                        .build();
            }

            case MONTH -> {
                LocalDate today = LocalDate.now(zone);
                LocalDate firstDay = today.withDayOfMonth(1);
                LocalDate nextMonth = firstDay.plusMonths(1);

                start = firstDay.atStartOfDay(zone).toInstant();
                end = nextMonth.atStartOfDay(zone).toInstant();

                depositRaw = depositOrderRepository.sumUserDepositByMonth(userId, start, end);
                withdrawRaw = withdrawOrderRepository.sumUserWithdrawByMonth(userId, start, end);

                return UserCashFlowResponse.builder()
                        .period(period)
                        .items(buildUserMonthItems(today.lengthOfMonth(), depositRaw, withdrawRaw))
                        .build();
            }

            case YEAR -> {
                LocalDate today = LocalDate.now(zone);
                LocalDate firstDay = LocalDate.of(today.getYear(), 1, 1);
                LocalDate nextYear = firstDay.plusYears(1);

                start = firstDay.atStartOfDay(zone).toInstant();
                end = nextYear.atStartOfDay(zone).toInstant();

                depositRaw = depositOrderRepository.sumUserDepositByYear(userId, start, end);
                withdrawRaw = withdrawOrderRepository.sumUserWithdrawByYear(userId, start, end);

                return UserCashFlowResponse.builder()
                        .period(period)
                        .items(buildUserYearItems(depositRaw, withdrawRaw))
                        .build();
            }

            default -> throw new IllegalArgumentException("Unsupported dashboard period");
        }
    }

    private List<UserCashFlowPointResponse> buildUserWeekItems(
            List<CashFlowProjection> depositRaw,
            List<CashFlowProjection> withdrawRaw
    ) {
        Map<Integer, BigDecimal> depositMap = toAmountMap(depositRaw);
        Map<Integer, BigDecimal> withdrawMap = toAmountMap(withdrawRaw);

        // MySQL DAYOFWEEK: Sunday=1, Monday=2 ... Saturday=7
        List<Integer> keys = List.of(2, 3, 4, 5, 6, 7, 1);
        List<String> labels = List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");

        List<UserCashFlowPointResponse> items = new ArrayList<>();

        for (int i = 0; i < keys.size(); i++) {
            Integer key = keys.get(i);

            items.add(UserCashFlowPointResponse.builder()
                    .label(labels.get(i))
                    .depositAmount(depositMap.getOrDefault(key, BigDecimal.ZERO))
                    .withdrawAmount(withdrawMap.getOrDefault(key, BigDecimal.ZERO))
                    .build());
        }

        return items;
    }

    private List<UserCashFlowPointResponse> buildUserMonthItems(
            int daysInMonth,
            List<CashFlowProjection> depositRaw,
            List<CashFlowProjection> withdrawRaw
    ) {
        Map<Integer, BigDecimal> depositMap = toAmountMap(depositRaw);
        Map<Integer, BigDecimal> withdrawMap = toAmountMap(withdrawRaw);

        List<UserCashFlowPointResponse> items = new ArrayList<>();

        for (int day = 1; day <= daysInMonth; day++) {
            items.add(UserCashFlowPointResponse.builder()
                    .label(String.format("%02d", day))
                    .depositAmount(depositMap.getOrDefault(day, BigDecimal.ZERO))
                    .withdrawAmount(withdrawMap.getOrDefault(day, BigDecimal.ZERO))
                    .build());
        }

        return items;
    }

    private List<UserCashFlowPointResponse> buildUserYearItems(
            List<CashFlowProjection> depositRaw,
            List<CashFlowProjection> withdrawRaw
    ) {
        Map<Integer, BigDecimal> depositMap = toAmountMap(depositRaw);
        Map<Integer, BigDecimal> withdrawMap = toAmountMap(withdrawRaw);

        List<String> labels = List.of(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        );

        List<UserCashFlowPointResponse> items = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            items.add(UserCashFlowPointResponse.builder()
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
