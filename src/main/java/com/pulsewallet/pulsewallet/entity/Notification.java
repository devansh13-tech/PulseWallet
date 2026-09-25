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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_notifications_user_unread", columnList = "user_id, read, created_at"),
        @Index(name = "idx_notifications_user_transaction_type", columnList = "user_id, transaction_id, type"),
        @Index(name = "idx_notifications_fraud_alert", columnList = "fraud_alert_id")
}, uniqueConstraints = @UniqueConstraint(name = "uk_notifications_fraud_alert", columnNames = { "fraud_alert_id" }))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fraud_alert_id")
    private FraudAlert fraudAlert;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected Notification() {
    }

    public Notification(User user, String title, String message, String type) {
        this(user, title, message, NotificationType.from(type), null, null);
    }

    public Notification(User user, String title, String message, NotificationType type) {
        this(user, title, message, type, null, null);
    }

    public Notification(User user, String title, String message, String type, Transaction transaction,
            FraudAlert fraudAlert) {
        this(user, title, message, NotificationType.from(type), transaction, fraudAlert);
    }

    public Notification(User user, String title, String message, NotificationType type, Transaction transaction,
            FraudAlert fraudAlert) {
        this.user = user;
        this.title = title;
        this.message = message;
        this.type = type == null ? NotificationType.GENERAL : type;
        this.transaction = transaction;
        this.fraudAlert = fraudAlert;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public Long getTransactionId() {
        return transaction != null ? transaction.getId() : null;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public FraudAlert getFraudAlert() {
        return fraudAlert;
    }

    public Long getFraudAlertId() {
        return fraudAlert != null ? fraudAlert.getId() : null;
    }

    public void setFraudAlert(FraudAlert fraudAlert) {
        this.fraudAlert = fraudAlert;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public NotificationType getType() {
        return type;
    }

    public boolean isRead() {
        return read;
    }

    public boolean getRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
        if (read && this.readAt == null) {
            this.readAt = Instant.now();
        }
        if (!read) {
            this.readAt = null;
        }
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }
}
