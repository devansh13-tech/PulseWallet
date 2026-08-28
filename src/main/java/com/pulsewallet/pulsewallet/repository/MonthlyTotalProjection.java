package com.pulsewallet.pulsewallet.repository;

import java.math.BigDecimal;

public interface MonthlyTotalProjection {
    Integer getYear();

    Integer getMonth();

    BigDecimal getTotal();
}
