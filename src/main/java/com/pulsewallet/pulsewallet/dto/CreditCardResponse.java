package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.pulsewallet.pulsewallet.entity.CreditCard;

public record CreditCardResponse(
        Long id,
        String issuer,
        String cardName,
        String lastFourDigits,
        BigDecimal creditLimit,
        BigDecimal currentOutstanding,
        LocalDate statementDate,
        LocalDate dueDate,
        String rewardsDescription,
        String benefitsDescription,
        Instant createdAt,
        Instant updatedAt) {

    public static CreditCardResponse from(CreditCard card) {
        return new CreditCardResponse(
                card.getId(),
                card.getIssuer(),
                card.getCardName(),
                card.getLastFourDigits(),
                card.getCreditLimit(),
                card.getCurrentOutstanding(),
                card.getStatementDate(),
                card.getDueDate(),
                card.getRewardsDescription(),
                card.getBenefitsDescription(),
                card.getCreatedAt(),
                card.getUpdatedAt());
    }
}
