package com.pulsewallet.pulsewallet.repository;

import java.math.BigDecimal;

public interface CategoryTotalProjection {
    Long getCategoryId();

    String getCategoryName();

    BigDecimal getTotal();
}
