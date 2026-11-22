package com.example.bank.service.wallet.user.creation;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;

public final class CardCreationPolicy {
    public static final BigDecimal MIN_AMOUNT = new BigDecimal("10.00");
    public static final BigDecimal FEE = new BigDecimal("1.00");
    private CardCreationPolicy() {}

    public static BigDecimal totalDebit(BigDecimal amount) {
        if (amount == null || amount.compareTo(MIN_AMOUNT) < 0 ||
                amount.stripTrailingZeros().scale() > 2 || amount.precision() - amount.scale() > 15) {
            throw new WalletException(MessageKeys.CARD_CREATION_INVALID_AMOUNT, HttpStatus.BAD_REQUEST);
        }
        return amount.add(FEE);
    }

    public static BigDecimal maxAmount(BigDecimal available) {
        return available.subtract(FEE).max(BigDecimal.ZERO)
                .setScale(2, java.math.RoundingMode.DOWN);
    }
}
