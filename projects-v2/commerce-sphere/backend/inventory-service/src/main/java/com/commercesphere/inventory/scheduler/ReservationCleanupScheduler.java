package com.commercesphere.inventory.scheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
public class ReservationCleanupScheduler {
    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredReservations() {
        // Redis TTL handles expiry automatically; this is a hook for future audit logic
    }
}
