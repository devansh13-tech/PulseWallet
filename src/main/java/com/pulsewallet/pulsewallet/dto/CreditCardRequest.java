package com.pulsewallet.pulsewallet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Inbound request for creating or updating a credit card.
 *
 * <p><b>Security:</b> there is intentionally no field for a full card number,
 * CVV, PIN, OTP, password, or any other sensitive credential.
 * {@code lastFourDigits} is validated to be exactly four numeric digits.
 */
public record CreditCardRequest(
        @NotBlank(message = "issuer must not be blank")
        String issuer,

        @NotBlank(message = "cardName must not be blank")
        @JsonAlias({"nickname", "card_name"})
        String cardName,

        @JsonAlias({"network", "card_network"})
        String cardNetwork,

        @NotNull(message = "lastFourDigits is required")
        @Pattern(regexp = "\\d{4}", message = "lastFourDigits must be exactly 4 numeric digits")
        @JsonAlias({"last_four_digits", "lastFour"})
        String lastFourDigits,

        @NotNull(message = "creditLimit is required")
        @DecimalMin(value = "0.01", message = "creditLimit must be greater than 0")
        @JsonAlias({"credit_limit", "limit"})
        BigDecimal creditLimit,

        @NotNull(message = "currentOutstanding is required")
        @DecimalMin(value = "0", message = "currentOutstanding must be zero or greater")
        @JsonAlias({"current_outstanding", "outstanding", "balance"})
        BigDecimal currentOutstanding,

        @DecimalMin(value = "0", message = "statementBalance must be zero or greater")
        @JsonAlias({"statement_balance", "statementAmount"})
        BigDecimal statementBalance,

        @DecimalMin(value = "0", message = "minimumDue must be zero or greater")
        @JsonAlias({"minimum_due", "minDue"})
        BigDecimal minimumDue,

        @NotNull(message = "statementDate is required")
        @JsonAlias({"statement_date", "billingCycleDate", "billing_cycle_date"})
        LocalDate statementDate,

        @NotNull(message = "dueDate is required")
        @JsonAlias({"due_date", "paymentDueDate", "payment_due_date"})
        LocalDate dueDate,

        @DecimalMin(value = "0", message = "annualFee must be zero or greater")
        @JsonAlias({"annual_fee", "fee"})
        BigDecimal annualFee,

        String dataSource,

        @JsonAlias({"rewards_description", "rewardInfo", "reward_info", "rewards"})
        String rewardsDescription,

        @JsonAlias({"benefits_description", "benefitInfo", "benefits"})
        String benefitsDescription) {

    /**
     * Backward-compatible 9-arg constructor for requests without cardNetwork or annualFee.
     */
    public CreditCardRequest(
            String issuer,
            String cardName,
            String lastFourDigits,
            BigDecimal creditLimit,
            BigDecimal currentOutstanding,
            LocalDate statementDate,
            LocalDate dueDate,
            String rewardsDescription,
            String benefitsDescription) {
        this(issuer, cardName, null, lastFourDigits, creditLimit, currentOutstanding,
                null, null, statementDate, dueDate, BigDecimal.ZERO, "MANUAL", rewardsDescription, benefitsDescription);
    }

    /**
     * Backward-compatible 11-arg constructor.
     */
    public CreditCardRequest(
            String issuer,
            String cardName,
            String cardNetwork,
            String lastFourDigits,
            BigDecimal creditLimit,
            BigDecimal currentOutstanding,
            LocalDate statementDate,
            LocalDate dueDate,
            BigDecimal annualFee,
            String rewardsDescription,
            String benefitsDescription) {
        this(issuer, cardName, cardNetwork, lastFourDigits, creditLimit, currentOutstanding,
                null, null, statementDate, dueDate, annualFee, "MANUAL", rewardsDescription, benefitsDescription);
    }
}
