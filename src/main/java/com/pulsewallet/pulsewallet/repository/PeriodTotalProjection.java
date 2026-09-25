package com.pulsewallet.pulsewallet.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface PeriodTotalProjection {
    LocalDate getPeriod();

    BigDecimal getTotal();
}
