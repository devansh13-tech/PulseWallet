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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "linked_payment_accounts")
public class LinkedPaymentAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "provider_name", nullable = false, length = 120)
    private String providerName;

    @Column(name = "account_name", nullable = false, length = 120)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    private LinkedPaymentAccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LinkedPaymentAccountStatus status;

    @Column(name = "masked_identifier", nullable = false, length = 80)
    private String maskedIdentifier;

    @Column(precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "account_subtype", length = 50)
    private String accountSubtype;

    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;

    @Column(name = "connected_at", nullable = false)
    private Instant connectedAt;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    protected LinkedPaymentAccount() {
    }

    public LinkedPaymentAccount(User user, String providerName, String accountName,
            LinkedPaymentAccountType accountType, String maskedIdentifier) {
        this(user, providerName, accountName, accountType, maskedIdentifier, BigDecimal.ZERO, null, null);
    }

    public LinkedPaymentAccount(User user, String providerName, String accountName,
            LinkedPaymentAccountType accountType, String maskedIdentifier, BigDecimal balance,
            String accountSubtype, String ifscCode) {
        this.user = user;
        this.providerName = providerName;
        this.accountName = accountName;
        this.accountType = accountType;
        this.status = LinkedPaymentAccountStatus.CONNECTED;
        this.maskedIdentifier = maskedIdentifier;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
        this.accountSubtype = accountSubtype;
        this.ifscCode = ifscCode;
    }

    @PrePersist
    void onCreate() {
        if (connectedAt == null)
            connectedAt = Instant.now();
        if (balance == null)
            balance = BigDecimal.ZERO;
    }

    public Long getId() {
        return id;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getAccountName() {
        return accountName;
    }

    public LinkedPaymentAccountType getAccountType() {
        return accountType;
    }

    public LinkedPaymentAccountStatus getStatus() {
        return status;
    }

    public String getMaskedIdentifier() {
        return maskedIdentifier;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance != null ? balance : BigDecimal.ZERO;
    }

    public String getAccountSubtype() {
        return accountSubtype;
    }

    public void setAccountSubtype(String accountSubtype) {
        this.accountSubtype = accountSubtype;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void touchSync() {
        this.lastSyncedAt = Instant.now();
    }

    public void disconnect() {
        status = LinkedPaymentAccountStatus.DISCONNECTED;
    }
}
