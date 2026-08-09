package com.commercemesh.notification.controller;

import com.commercemesh.notification.dto.NotificationResponse;
import com.commercemesh.notification.dto.PageResponse;
import com.commercemesh.notification.security.JwtTokenProvider;
import com.commercemesh.notification.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;
    private final JwtTokenProvider jwtTokenProvider;

    public NotificationController(NotificationService notificationService, JwtTokenProvider jwtTokenProvider) {
        this.notificationService = notificationService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> getUserNotifications(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {

        String userId = extractUserId(request);
        log.debug("Fetching notifications for userId={}", userId);
        PageResponse<NotificationResponse> response = notificationService.getNotifications(userId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getNotificationById(
            @PathVariable String id,
            HttpServletRequest request) {

        String userId = extractUserId(request);
        log.debug("Fetching notification id={} for userId={}", id, userId);
        NotificationResponse response = notificationService.getNotificationById(id);

        // Ensure the notification belongs to the requesting user
        if (!response.userId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/resend")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ADMIN')")
    public ResponseEntity<NotificationResponse> resendFailedNotification(@PathVariable String id) {
        log.info("Resending failed notification id={}", id);
        NotificationResponse response = notificationService.resendFailedNotification(id);
        return ResponseEntity.ok(response);
    }

    private String extractUserId(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            return jwtTokenProvider.getUserIdFromToken(token);
        }
        throw new IllegalStateException("No valid JWT token found in request");
    }
}
