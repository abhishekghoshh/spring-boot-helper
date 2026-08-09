package com.commercemesh.notification.dto;

import com.commercemesh.notification.entity.Notification;
import com.commercemesh.notification.enumeration.NotificationChannel;
import com.commercemesh.notification.enumeration.NotificationStatus;
import com.commercemesh.notification.enumeration.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        String id,
        String userId,
        String orderId,
        NotificationType type,
        NotificationChannel channel,
        String recipient,
        String subject,
        String body,
        NotificationStatus status,
        LocalDateTime sentAt,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getOrderId(),
                notification.getType(),
                notification.getChannel(),
                notification.getRecipient(),
                notification.getSubject(),
                notification.getBody(),
                notification.getStatus(),
                notification.getSentAt(),
                notification.getCreatedAt()
        );
    }
}
