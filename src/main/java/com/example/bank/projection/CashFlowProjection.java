package com.example.bank.projection;


import java.math.BigDecimal;

public interface CashFlowProjection {

    Integer getGroupKey();

    BigDecimal getAmount();
}