package com.commercemesh.notification.repository;

import com.commercemesh.notification.entity.Notification;
import com.commercemesh.notification.enumeration.NotificationStatus;
import com.commercemesh.notification.enumeration.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    List<Notification> findByOrderId(String orderId);

    List<Notification> findByStatus(NotificationStatus status);

    Optional<Notification> findByUserIdAndOrderIdAndType(String userId, String orderId, NotificationType type);
}
