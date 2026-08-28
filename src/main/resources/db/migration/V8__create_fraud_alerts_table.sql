CREATE TABLE fraud_alerts (
    id BIGSERIAL PRIMARY KEY,

    transaction_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    fraud_probability NUMERIC(10, 8) NOT NULL,
    risk_score NUMERIC(10, 8) NOT NULL,

    risk_level VARCHAR(20) NOT NULL,

    resolved BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_fraud_alert_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES transactions(id),

    CONSTRAINT fk_fraud_alert_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);

CREATE INDEX idx_fraud_alerts_user_created
    ON fraud_alerts(user_id, created_at);

CREATE INDEX idx_fraud_alerts_transaction
    ON fraud_alerts(transaction_id);

CREATE INDEX idx_fraud_alerts_unresolved
    ON fraud_alerts(user_id, resolved);