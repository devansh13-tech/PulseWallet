CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,
    transaction_id BIGINT,
    fraud_alert_id BIGINT,

    title VARCHAR(160) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    type VARCHAR(40) NOT NULL,
    read BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    read_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_notifications_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES transactions(id) ON DELETE SET NULL,

    CONSTRAINT fk_notifications_fraud_alert
        FOREIGN KEY (fraud_alert_id)
        REFERENCES fraud_alerts(id) ON DELETE SET NULL,

    CONSTRAINT chk_notifications_type
        CHECK (type IN ('FRAUD_ALERT', 'BUDGET', 'SYSTEM', 'GENERAL'))
);

CREATE INDEX idx_notifications_user_created
    ON notifications(user_id, created_at DESC);

CREATE INDEX idx_notifications_user_unread
    ON notifications(user_id, read, created_at DESC);

CREATE INDEX idx_notifications_user_transaction_type
    ON notifications(user_id, transaction_id, type);
