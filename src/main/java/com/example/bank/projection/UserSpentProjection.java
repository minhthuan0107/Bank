package com.example.bank.projection;

import java.math.BigDecimal;

public interface UserSpentProjection {

    Long getUserId();

    BigDecimal getTotalSpent();
}
