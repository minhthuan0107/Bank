package com.example.bank.repository.projection;

import com.example.bank.enums.wallet.CardTxnStatus;
import com.example.bank.enums.wallet.CardTxnType;

import java.math.BigDecimal;
import java.time.Instant;

public interface CardFundingTransactionProjection {

    Long getId();

    String getBin();

    String getLast4();

    CardTxnType getType();

    BigDecimal getAmount();

    CardTxnStatus getStatus();

    Instant getCreatedAt();
}