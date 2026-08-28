package com.pulsewallet.pulsewallet.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/**
 * A single income or expense entry belonging to exactly one {@link User}.
 *
 * <p>
 * {@code amount} is always positive; {@link TransactionType} carries the
 * direction (INCOME vs EXPENSE) rather than the sign of the number, so a
 * dashboard summing "money in" never has to remember a sign convention.
 * {@code category} may be {@code null} - an uncategorized transaction is
 * valid and common right after import.
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transactions_user_date", columnList = "user_id, transaction_date"),
        @Index(name = "idx_transactions_user_category", columnList = "user_id, category_id")
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    @Column(length = 160)
    private String merchant;

    @Column(name = "payment_channel", length = 40)
    private String paymentChannel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Transaction() {
    }

    public Transaction(
            User user,
            BigDecimal amount,
            String description,
            Category category,
            TransactionType type,
            LocalDate transactionDate) {
        this(user, amount, description, null, null, category, type, transactionDate);
    }

    public Transaction(
            User user,
            BigDecimal amount,
            String description,
            String merchant,
            String paymentChannel,
            Category category,
            TransactionType type,
            LocalDate transactionDate) {
        this.user = user;
        this.amount = amount;
        this.description = description;
        this.merchant = merchant;
        this.paymentChannel = paymentChannel;
        this.category = category;
        this.type = type;
        this.transactionDate = transactionDate;
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

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    public String getPaymentChannel() {
        return paymentChannel;
    }

    public void setPaymentChannel(String paymentChannel) {
        this.paymentChannel = paymentChannel;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
