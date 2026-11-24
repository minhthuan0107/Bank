package com.example.bank.service.wallet.infini;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.entity.wallet.infini.InfiniFundingOrder;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Rules for Infini card top-ups and withdrawals: no fee, top-up of at least 10 USD, withdrawal of any
 * amount as long as the card keeps at least 1 USD.
 */
public final class InfiniFundingPolicy {
    public static final BigDecimal TOP_UP_MIN = new BigDecimal("10.00");
    public static final BigDecimal MIN_CARD_BALANCE = new BigDecimal("1.00");

    /** Infini refuses withdrawals within 30 minutes after a top-up (code 21026); one minute of margin. */
    public static final Duration WITHDRAW_COOLDOWN = Duration.ofMinutes(31);

    private InfiniFundingPolicy() {}

    public static void validate(String type, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 ||
                amount.stripTrailingZeros().scale() > 2 || amount.precision() - amount.scale() > 15) {
            throw new WalletException(MessageKeys.INFINI_FUNDING_INVALID_AMOUNT, HttpStatus.BAD_REQUEST);
        }
        if (InfiniFundingOrder.TOP_UP.equals(type) && amount.compareTo(TOP_UP_MIN) < 0) {
            throw new WalletException(MessageKeys.INFINI_FUNDING_TOPUP_MIN, HttpStatus.BAD_REQUEST);
        }
    }

    /** cardBalance is the balance just read from Infini. */
    public static void validateWithdrawal(BigDecimal cardBalance, BigDecimal amount) {
        if (cardBalance == null || cardBalance.compareTo(amount) < 0) {
            throw new WalletException(MessageKeys.INSUFFICIENT_CARD_BALANCE, HttpStatus.BAD_REQUEST);
        }
        if (cardBalance.subtract(amount).compareTo(MIN_CARD_BALANCE) < 0) {
            throw new WalletException(MessageKeys.INFINI_FUNDING_MIN_CARD_BALANCE, HttpStatus.BAD_REQUEST);
        }
    }
}
