package com.crimsonlogic.busticketbooking.scheduler;

import com.crimsonlogic.busticketbooking.service.SeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background task that runs every 60 seconds to release
 * any seat locks whose 10-minute expiry has passed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatLockScheduler {

    private final SeatService seatService;

    @Scheduled(fixedDelay = 60_000)
    public void releaseExpiredSeatLocks() {
        log.debug("Running expired seat lock cleanup...");
        seatService.releaseExpiredLocks();
    }
}
