package com.makershub.scheduler;

import com.makershub.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderScheduler {

    private final OrderService orderService;

    /**
     * Automatically completes delivered orders that have passed their 48-hour quality check window.
     * Runs every hour on the hour.
     */
    @Scheduled(cron = "${makershub.scheduler.auto-complete-cron:0 0 * * * *}")
    public void runAutoComplete() {
        log.info("Starting scheduled auto-completion of expired orders...");
        try {
            orderService.autoCompleteExpiredOrders();
            log.info("Scheduled auto-completion finished successfully.");
        } catch (Exception ex) {
            log.error("Failed to run scheduled auto-completion of expired orders", ex);
        }
    }
}
