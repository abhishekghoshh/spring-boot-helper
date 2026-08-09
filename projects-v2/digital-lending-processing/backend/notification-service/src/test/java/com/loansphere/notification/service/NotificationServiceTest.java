package com.loansphere.notification.service;

import com.loansphere.notification.model.Notification;
import com.loansphere.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void shouldSendEmailAndSaveNotification() {
        notificationService.sendEmail("user1", "user@test.com", "Subject", "Body");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        verify(mailSender).send(any(SimpleMailMessage.class));

        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo("user1");
        assertThat(saved.getType()).isEqualTo("EMAIL");
        assertThat(saved.getStatus()).isEqualTo("SENT");
    }

    @Test
    void shouldSaveFailedStatusWhenEmailFails() {
        doThrow(new RuntimeException("Mail server down")).when(mailSender).send(any(SimpleMailMessage.class));

        notificationService.sendEmail("user1", "user@test.com", "Subject", "Body");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
    }

    @Test
    void shouldSendSms() {
        notificationService.sendSms("user1", "+1234567890", "Hello");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("SMS");
        assertThat(captor.getValue().getRecipient()).isEqualTo("+1234567890");
    }

    @Test
    void shouldGetUserNotifications() {
        Notification n1 = new Notification();
        n1.setUserId("user1");
        n1.setTitle("Test");

        when(notificationRepository.findByUserId("user1")).thenReturn(List.of(n1));

        List<Notification> notifications = notificationService.getUserNotifications("user1");

        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getUserId()).isEqualTo("user1");
    }

    @Test
    void shouldMarkNotificationAsRead() {
        Notification n = new Notification();
        n.setRead(false);

        when(notificationRepository.findById("notif1")).thenReturn(Optional.of(n));
        when(notificationRepository.save(any(Notification.class))).thenReturn(n);

        notificationService.markAsRead("notif1");

        assertThat(n.isRead()).isTrue();
        verify(notificationRepository).save(n);
    }

    @Test
    void shouldHandleMarkAsReadWhenNotFound() {
        when(notificationRepository.findById("missing")).thenReturn(Optional.empty());

        notificationService.markAsRead("missing");

        verify(notificationRepository, never()).save(any());
    }
}
