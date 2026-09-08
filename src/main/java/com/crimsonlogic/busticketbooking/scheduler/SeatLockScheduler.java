package com.crimsonlogic.busticketbooking.scheduler;

import com.crimsonlogic.busticketbooking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background task that runs every 60 seconds to expire
 * pending bookings whose 10-minute payment window has passed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatLockScheduler {

    private final BookingService bookingService;

    @Scheduled(fixedDelay = 60_000)
    public void releaseExpiredSeatLocks() {
        log.debug("Running booking expiration cleanup...");
        bookingService.expireBookings();
    }
}
