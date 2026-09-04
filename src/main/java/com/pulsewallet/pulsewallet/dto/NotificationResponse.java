package com.pulsewallet.pulsewallet.dto;

import java.time.Instant;

import com.pulsewallet.pulsewallet.entity.Notification;

public record NotificationResponse(
        Long id,
        String title,
        String message,
        String type,
        boolean read,
        Instant createdAt,
        Instant readAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType().name(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReadAt());
    }
}
