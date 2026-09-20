package com.crimsonlogic.busticketbooking.scheduler;

import com.crimsonlogic.busticketbooking.service.BusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task that auto-deactivates buses which have not operated any trip
 * in the last 10 days.
 *
 * Runs daily at midnight (00:00).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BusActivationScheduler {

    private final BusService busService;

    /**
     * Auto-deactivate buses inactive for 10+ days.
     * Cron: every day at midnight.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void autoDeactivateInactiveBuses() {
        log.info("[BusActivationScheduler] Running daily auto-deactivation check...");
        try {
            busService.autoDeactivateInactiveBuses();
            log.info("[BusActivationScheduler] Auto-deactivation check complete.");
        } catch (Exception e) {
            log.error("[BusActivationScheduler] Error during auto-deactivation: {}", e.getMessage(), e);
        }
    }
}
