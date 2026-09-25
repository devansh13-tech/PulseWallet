package com.pulsewallet.pulsewallet.repository;

import java.math.BigDecimal;

public interface CategoryMonthlyTotalProjection {
    Integer getYear();

    Integer getMonth();

    Long getCategoryId();

    String getCategoryName();

    BigDecimal getTotal();
}
