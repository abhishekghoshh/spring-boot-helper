package com.commercesphere.order.scheduler;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
public class OrderCleanupScheduler {
    private static final Logger log = LoggerFactory.getLogger(OrderCleanupScheduler.class);
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldOrders() { log.info("Order cleanup job triggered"); }
}
