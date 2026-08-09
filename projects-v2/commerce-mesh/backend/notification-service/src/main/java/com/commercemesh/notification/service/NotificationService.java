package com.commercemesh.notification.service;

import com.commercemesh.notification.dto.NotificationResponse;
import com.commercemesh.notification.dto.PageResponse;
import com.commercemesh.notification.entity.Notification;
import com.commercemesh.notification.enumeration.NotificationChannel;
import com.commercemesh.notification.enumeration.NotificationStatus;
import com.commercemesh.notification.enumeration.NotificationType;
import com.commercemesh.notification.exception.ResourceNotFoundException;
import com.commercemesh.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    public NotificationService(NotificationRepository notificationRepository, EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
    }

    /**
     * Create and send a notification. Saves to DB first, then dispatches via the
     * appropriate channel (currently email-only mock).
     */
    public NotificationResponse sendNotification(String userId, String orderId, NotificationType type,
                                                  NotificationChannel channel, String recipient,
                                                  String subject, String body) {
        Notification notification = new Notification(userId, orderId, type, channel, recipient, subject, body);
        notification = notificationRepository.save(notification);
        log.info("Notification created: id={}, type={}, channel={}", notification.getId(), type, channel);

        dispatch(notification);

        return NotificationResponse.from(notification);
    }

    /**
     * Get paginated notifications for a specific user.
     */
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(String userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        Page<NotificationResponse> responsePage = page.map(NotificationResponse::from);
        return PageResponse.from(responsePage);
    }

    /**
     * Get a single notification by id.
     */
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(String id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
        return NotificationResponse.from(notification);
    }

    /**
     * Resend a previously failed notification.
     */
    public NotificationResponse resendFailedNotification(String id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));

        if (notification.getStatus() != NotificationStatus.FAILED) {
            throw new IllegalStateException("Only failed notifications can be resent. Current status: " + notification.getStatus());
        }

        log.info("Resending failed notification: id={}, type={}", notification.getId(), notification.getType());
        notification.setStatus(NotificationStatus.PENDING);
        notification = notificationRepository.save(notification);

        dispatch(notification);

        return NotificationResponse.from(notification);
    }

    /**
     * Check if a notification already exists for the given user+order+type combination
     * (used for idempotent consumer).
     */
    @Transactional(readOnly = true)
    public boolean existsByUserOrderAndType(String userId, String orderId, NotificationType type) {
        return notificationRepository.findByUserIdAndOrderIdAndType(userId, orderId, type).isPresent();
    }

    /**
     * Dispatch notification through the appropriate channel.
     */
    private void dispatch(Notification notification) {
        switch (notification.getChannel()) {
            case EMAIL -> emailService.send(notification);
            case SMS -> {
                log.info("SMS channel not yet implemented for notification id={}. Marking as sent (mock).", notification.getId());
                notification.markSent();
                notificationRepository.save(notification);
            }
            case PUSH -> {
                log.info("PUSH channel not yet implemented for notification id={}. Marking as sent (mock).", notification.getId());
                notification.markSent();
                notificationRepository.save(notification);
            }
        }
    }
}
