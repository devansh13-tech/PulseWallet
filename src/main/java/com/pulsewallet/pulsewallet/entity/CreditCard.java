package com.pulsewallet.pulsewallet.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Safe credit-card metadata for a {@link User}.
 *
 * <p><b>Security:</b> this entity intentionally has <em>no</em> fields for a
 * full card number, CVV, PIN, OTP, banking password, or any other sensitive
 * credential. {@code lastFourDigits} is the only card-number-related datum
 * stored — it is exactly four numeric digits and is validated at both the DTO
 * and database levels.
 */
@Entity
@Table(name = "credit_cards", indexes = @Index(name = "idx_credit_cards_user", columnList = "user_id"))
public class CreditCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 120)
    private String issuer;

    @Column(name = "card_name", nullable = false, length = 120)
    private String cardName;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "last_four_digits", nullable = false, length = 4)
    private String lastFourDigits;

    @Column(name = "credit_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "current_outstanding", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentOutstanding;

    @Column(name = "statement_date", nullable = false)
    private LocalDate statementDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "rewards_description", length = 500)
    private String rewardsDescription;

    @Column(name = "benefits_description", length = 500)
    private String benefitsDescription;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** JPA requires a no-arg constructor; keep it non-public. */
    protected CreditCard() {
    }

    public CreditCard(User user, String issuer, String cardName, String lastFourDigits,
            BigDecimal creditLimit, BigDecimal currentOutstanding,
            LocalDate statementDate, LocalDate dueDate,
            String rewardsDescription, String benefitsDescription) {
        this.user = user;
        this.issuer = issuer;
        this.cardName = cardName;
        this.lastFourDigits = lastFourDigits;
        this.creditLimit = creditLimit;
        this.currentOutstanding = currentOutstanding;
        this.statementDate = statementDate;
        this.dueDate = dueDate;
        this.rewardsDescription = rewardsDescription;
        this.benefitsDescription = benefitsDescription;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // --- Getters ---

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getCardName() {
        return cardName;
    }

    public String getLastFourDigits() {
        return lastFourDigits;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public BigDecimal getCurrentOutstanding() {
        return currentOutstanding;
    }

    public LocalDate getStatementDate() {
        return statementDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public String getRewardsDescription() {
        return rewardsDescription;
    }

    public String getBenefitsDescription() {
        return benefitsDescription;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // --- Setters (for updates) ---

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public void setLastFourDigits(String lastFourDigits) {
        this.lastFourDigits = lastFourDigits;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public void setCurrentOutstanding(BigDecimal currentOutstanding) {
        this.currentOutstanding = currentOutstanding;
    }

    public void setStatementDate(LocalDate statementDate) {
        this.statementDate = statementDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public void setRewardsDescription(String rewardsDescription) {
        this.rewardsDescription = rewardsDescription;
    }

    public void setBenefitsDescription(String benefitsDescription) {
        this.benefitsDescription = benefitsDescription;
    }
}
