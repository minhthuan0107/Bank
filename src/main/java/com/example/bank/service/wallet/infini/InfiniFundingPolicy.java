package com.example.bank.service.wallet.infini;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

/** Customer pricing for Infini card top-ups and withdrawals: no fee, at least 10 USD. */
public final class InfiniFundingPolicy {
    public static final BigDecimal MIN_AMOUNT = new BigDecimal("10.00");

    private InfiniFundingPolicy() {}

    public static void validate(BigDecimal amount) {
        if (amount == null || amount.compareTo(MIN_AMOUNT) < 0 ||
                amount.stripTrailingZeros().scale() > 2 || amount.precision() - amount.scale() > 15) {
            throw new WalletException(MessageKeys.INFINI_FUNDING_INVALID_AMOUNT, HttpStatus.BAD_REQUEST);
        }
    }
}
