package com.loansphere.notification.service;

import com.loansphere.notification.model.Notification;
import com.loansphere.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    @Async
    public void sendEmail(String userId, String recipient, String subject, String body) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(subject);
        notification.setMessage(body);
        notification.setType("EMAIL");
        notification.setRecipient(recipient);
        notification.setStatus("PENDING");
        notification.setRead(false);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            notification.setStatus("SENT");
            log.info("Email sent to {}", recipient);
        } catch (Exception e) {
            notification.setStatus("FAILED");
            log.error("Failed to send email to {}: {}", recipient, e.getMessage());
        }

        notificationRepository.save(notification);
    }

    @Async
    public void sendSms(String userId, String phone, String message) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle("SMS");
        notification.setMessage(message);
        notification.setType("SMS");
        notification.setRecipient(phone);
        notification.setStatus("SENT");
        notification.setRead(false);
        notificationRepository.save(notification);
        log.info("SMS sent to {}: {}", phone, message);
    }

    public List<Notification> getUserNotifications(String userId) {
        return notificationRepository.findByUserId(userId);
    }

    public void markAsRead(String id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }
}
