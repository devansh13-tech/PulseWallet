package com.pulsewallet.pulsewallet.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "fraud_alerts")
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "fraud_probability", nullable = false, precision = 10, scale = 8)
    private BigDecimal fraudProbability;

    @Column(name = "risk_score", nullable = false, precision = 10, scale = 8)
    private BigDecimal riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(nullable = false)
    private boolean resolved = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FraudAlert() {
    }

    public FraudAlert(
            Transaction transaction,
            User user,
            BigDecimal fraudProbability,
            BigDecimal riskScore,
            RiskLevel riskLevel) {

        this.transaction = transaction;
        this.user = user;
        this.fraudProbability = fraudProbability;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
    }

    @jakarta.persistence.PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public User getUser() {
        return user;
    }

    public BigDecimal getFraudProbability() {
        return fraudProbability;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}