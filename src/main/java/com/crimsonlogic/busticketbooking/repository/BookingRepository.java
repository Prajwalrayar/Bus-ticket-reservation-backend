package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.Booking;
import com.crimsonlogic.busticketbooking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {

    Optional<Booking> findByBookingReferenceIgnoreCase(
            String bookingReference
    );

    List<Booking> findByBookedByUser_UserId(
            String userId
    );

    long countByBookingStatus(BookingStatus bookingStatus);

    long countByTrip_Route_RouteIdAndBookingStatus(
            String routeId, 
            BookingStatus status
    );

    /*
     * Count active (CONFIRMED or PENDING) bookings for a specific trip.
     * Used by Phase 6 to block destructive trip modifications.
     */
    long countByTrip_TripIdAndBookingStatusIn(
            String tripId,
            List<BookingStatus> statuses
    );

    /*
     * Count active (CONFIRMED or PENDING) bookings for any FUTURE trip
     * operated by a given bus.
     * "Future" = trip's travel date is strictly after today.
     * Used by Phase 6 to block destructive bus modifications.
     */
    @Query("SELECT COUNT(b) FROM Booking b " +
           "WHERE b.trip.bus.busId = :busId " +
           "AND b.bookingStatus IN :statuses " +
           "AND b.trip.travelDate > CURRENT_DATE")
    long countActiveFutureBookingsByBusId(
            @Param("busId") String busId,
            @Param("statuses") List<BookingStatus> statuses
    );

    List<Booking> findByTrip_Bus_Operator_OperatorId(
            String operatorId
    );

    /*
     * Count active (CONFIRMED or PENDING) bookings for any FUTURE trip
     * operating on a given route.
     * "Future" = trip's travel date is strictly after today.
     * Used by Phase 6 to block destructive route modifications.
     */
    @Query("SELECT COUNT(b) FROM Booking b " +
           "WHERE b.trip.route.routeId = :routeId " +
           "AND b.bookingStatus IN :statuses " +
           "AND b.trip.travelDate > CURRENT_DATE")
    long countActiveFutureBookingsByRouteId(
            @Param("routeId") String routeId,
            @Param("statuses") List<BookingStatus> statuses
    );

    List<Booking> findByBookingStatusAndExpiryTimeBefore(BookingStatus status, java.time.LocalDateTime time);

}

