package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExtractedTransactionDto(
        LocalDate date,
        String description,
        BigDecimal amount,
        String type) {
}
