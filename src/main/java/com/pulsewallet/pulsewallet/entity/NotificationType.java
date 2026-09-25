package com.pulsewallet.pulsewallet.entity;

public enum NotificationType {
    FRAUD_ALERT,
    BUDGET,
    SYSTEM,
    GENERAL;

    public static NotificationType from(String value) {
        if (value == null || value.isBlank()) {
            return GENERAL;
        }
        try {
            return NotificationType.valueOf(value.trim());
        } catch (IllegalArgumentException exception) {
            return GENERAL;
        }
    }
}
