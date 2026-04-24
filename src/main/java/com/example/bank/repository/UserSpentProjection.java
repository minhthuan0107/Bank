package com.example.bank.repository;

import java.math.BigDecimal;

public interface UserSpentProjection {

    Long getUserId();

    BigDecimal getTotalSpent();
}
