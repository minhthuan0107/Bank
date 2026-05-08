package com.example.bank.service.dashboard.impl;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.dto.response.wallet.user.UserAssetAllocationItemResponse;
import com.example.bank.dto.response.wallet.user.UserAssetAllocationResponse;
import com.example.bank.enums.dashboard.AssetAllocationKey;
import com.example.bank.projection.WalletAssetAllocationProjection;
import com.example.bank.repository.wallet.WalletRepository;
import com.example.bank.service.dashboard.UserDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDashboardServiceImpl implements UserDashboardService {

    private final WalletRepository walletRepository;


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
}
