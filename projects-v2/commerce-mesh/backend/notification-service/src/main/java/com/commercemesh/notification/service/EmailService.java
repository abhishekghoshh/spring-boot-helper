package com.commercemesh.notification.service;

import com.commercemesh.notification.entity.Notification;
import com.commercemesh.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final NotificationRepository notificationRepository;

    public EmailService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Mock email sending — logs the email content and marks the notification
     * as SENT. In a real implementation this would connect to an SMTP server
     * or use a service like SendGrid / Amazon SES.
     */
    public void send(Notification notification) {
        log.info("========== SENDING EMAIL ==========");
        log.info("To:      {}", notification.getRecipient());
        log.info("Subject: {}", notification.getSubject());
        log.info("Body:    {}{}", notification.getBody().substring(0, Math.min(100, notification.getBody().length())),
                notification.getBody().length() > 100 ? "..." : "");
        log.info("===================================");

        try {
            // Simulate sending delay
            Thread.sleep(200);

            notification.markSent();
            notificationRepository.save(notification);

            log.info("Email successfully sent for notification id={}, recipient={}",
                    notification.getId(), notification.getRecipient());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            notification.markFailed();
            notificationRepository.save(notification);
            throw new RuntimeException("Email sending interrupted", e);
        }
    }
}
