package com.pulsewallet.pulsewallet.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "investment_profiles")
public class InvestmentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_preference", nullable = false, length = 20)
    private InvestmentRiskPreference riskPreference;

    @Column(name = "time_horizon_years", nullable = false)
    private Integer timeHorizonYears;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InvestmentProfile() {
    }

    public InvestmentProfile(User user, InvestmentRiskPreference riskPreference, Integer timeHorizonYears) {
        this.user = user;
        this.riskPreference = riskPreference;
        this.timeHorizonYears = timeHorizonYears;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public InvestmentRiskPreference getRiskPreference() {
        return riskPreference;
    }

    public Integer getTimeHorizonYears() {
        return timeHorizonYears;
    }

    public void setRiskPreference(InvestmentRiskPreference riskPreference) {
        this.riskPreference = riskPreference;
    }

    public void setTimeHorizonYears(Integer timeHorizonYears) {
        this.timeHorizonYears = timeHorizonYears;
    }
}
