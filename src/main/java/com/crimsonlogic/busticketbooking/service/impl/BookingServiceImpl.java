package com.crimsonlogic.busticketbooking.service.impl;

import com.crimsonlogic.busticketbooking.dto.BookingCancelRequest;
import com.crimsonlogic.busticketbooking.dto.BookingCreateRequest;
import com.crimsonlogic.busticketbooking.dto.BookingDTO;
import com.crimsonlogic.busticketbooking.dto.BookingSeatDTO;
import com.crimsonlogic.busticketbooking.dto.PassengerDTO;
import com.crimsonlogic.busticketbooking.entity.Booking;
import com.crimsonlogic.busticketbooking.entity.BookingSeat;
import com.crimsonlogic.busticketbooking.entity.RouteStop;
import com.crimsonlogic.busticketbooking.entity.Trip;
import com.crimsonlogic.busticketbooking.entity.TripSeat;
import com.crimsonlogic.busticketbooking.entity.User;
import com.crimsonlogic.busticketbooking.entity.UserRole;
import com.crimsonlogic.busticketbooking.enums.BookingStatus;
import com.crimsonlogic.busticketbooking.enums.SeatStatus;
import com.crimsonlogic.busticketbooking.repository.BookingRepository;
import com.crimsonlogic.busticketbooking.repository.RouteStopRepository;
import com.crimsonlogic.busticketbooking.repository.TripRepository;
import com.crimsonlogic.busticketbooking.repository.TripSeatRepository;
import com.crimsonlogic.busticketbooking.repository.UserRepository;
import com.crimsonlogic.busticketbooking.service.BookingService;
import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final TripSeatRepository tripSeatRepository;
    private final RouteStopRepository routeStopRepository;
    private final UserRepository userRepository;
    private final EntityIdGenerator entityIdGenerator;


    // =========================================================
    // CREATE BOOKING
    // =========================================================

    @Override
    public BookingDTO createBooking(
            BookingCreateRequest request) {

        /*
         * The user is obtained from the authenticated
         * Spring Security context.
         *
         * Do NOT accept userId from the request.
         */
        User user = getAuthenticatedUser();


        /*
         * Find the requested trip.
         */
        Trip trip = tripRepository.findById(
                        request.getTripId()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Trip not found"
                        )
                );


        /*
         * Validate whether the trip can be booked.
         */
        validateTripForBooking(trip);


        /*
         * Find boarding point.
         */
        RouteStop boardingPoint =
                routeStopRepository.findById(
                                request.getBoardingPointId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Boarding point not found"
                                )
                        );


        /*
         * Find dropping point.
         */
        RouteStop droppingPoint =
                routeStopRepository.findById(
                                request.getDroppingPointId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Dropping point not found"
                                )
                        );


        /*
         * Validate that both stops belong to
         * this trip's route and that the boarding
         * stop occurs before the dropping stop.
         */
        validateRouteStops(
                trip,
                boardingPoint,
                droppingPoint
        );


        /*
         * Create Booking aggregate root.
         */
        Booking booking = new Booking();

        booking.setBookingId(
                entityIdGenerator.generate(EntityIdGenerator.PREFIX_BOOKING, bookingRepository::existsById)
        );

        booking.setBookingReference(
                generateBookingReference()
        );

        booking.setBookingStatus(
                BookingStatus.PENDING
        );

        booking.setBookedByUser(user);

        booking.setTrip(trip);

        booking.setBoardingPoint(
                boardingPoint
        );

        booking.setDroppingPoint(
                droppingPoint
        );

        booking.setTotalSeats(
                request.getPassengers().size()
        );

        booking.setDiscountAmount(
                BigDecimal.ZERO
        );

        booking.setTaxAmount(
                BigDecimal.ZERO
        );

        booking.setInsuranceAmount(
                BigDecimal.ZERO
        );

        booking.setOfferCodeUsed(
                request.getOfferCode()
        );


        /*
         * Create BookingSeat records.
         */
        List<BookingSeat> bookingSeats =
                new ArrayList<>();

        BigDecimal baseFareTotal =
                BigDecimal.ZERO;


        for (PassengerDTO passenger :
                request.getPassengers()) {

            /*
             * Find the trip-specific seat
             * using the passenger's selected seat.
             */
            TripSeat tripSeat =
                    findTripSeat(
                            trip,
                            passenger
                    );


            /*
             * Validate seat availability.
             */
            validateTripSeatForBooking(
                    tripSeat,
                    user
            );


            /*
             * Prevent selecting the same seat
             * twice in the same booking.
             */
            boolean duplicateSeat =
                    bookingSeats.stream()
                            .anyMatch(
                                    bookingSeat ->
                                            bookingSeat
                                                    .getTripSeat()
                                                    .getTripSeatId()
                                                    .equals(
                                                            tripSeat
                                                                    .getTripSeatId()
                                                    )
                            );

            if (duplicateSeat) {

                throw new IllegalArgumentException(
                        "Seat '"
                                + passenger.getSeatNumber()
                                + "' has been selected more than once"
                );
            }


            /*
             * Create BookingSeat.
             */
            BookingSeat bookingSeat =
                    createBookingSeat(
                            booking,
                            tripSeat,
                            passenger
                    );

            bookingSeats.add(
                    bookingSeat
            );


            /*
             * Calculate base fare using intermediate stops if available.
             */
            BigDecimal seatFare = tripSeat.getSeatFare();
            if (trip.getStopFares() != null && !trip.getStopFares().isEmpty()) {
                BigDecimal sourceFare = BigDecimal.ZERO;
                BigDecimal destFare = trip.getBaseFare();

                for (com.crimsonlogic.busticketbooking.entity.TripStopFare tsf : trip.getStopFares()) {
                    if (tsf.getRouteStop().getRouteStopId().equals(boardingPoint.getRouteStopId())) {
                        sourceFare = tsf.getFareFromSource();
                    }
                    if (tsf.getRouteStop().getRouteStopId().equals(droppingPoint.getRouteStopId())) {
                        destFare = tsf.getFareFromSource();
                    }
                }

                BigDecimal segmentFare = destFare.subtract(sourceFare);
                if (segmentFare.compareTo(BigDecimal.ZERO) > 0) {
                    seatFare = segmentFare;
                }
            }

            baseFareTotal =
                    baseFareTotal.add(
                            seatFare
                    );
        }


        /*
         * Store calculated base fare.
         */
        booking.setBaseFareTotal(
                baseFareTotal
        );


        /*
         * At this stage:
         *
         * total =
         * base fare
         *
         * Discount, tax, insurance and offer
         * calculations will be integrated later
         * with their respective services.
         */
        booking.setTotalAmount(
                baseFareTotal
        );


        /*
         * Attach BookingSeat children to Booking.
         */
        booking.setBookingSeats(
                bookingSeats
        );


        /*
         * Save Booking aggregate.
         *
         * Booking has CascadeType.ALL for BookingSeat,
         * so BookingSeat records are persisted together.
         */
        Booking savedBooking =
                bookingRepository.save(
                        booking
                );


        return convertToDTO(
                savedBooking
        );
    }


    // =========================================================
    // GET BOOKING BY ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public BookingDTO getBookingById(
            String bookingId) {

        User currentUser = getAuthenticatedUser();

        Booking booking =
                bookingRepository.findById(
                                bookingId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Booking not found"
                                )
                        );

        boolean isAdmin = currentUser.getUserRoles().stream()
                .map(UserRole::getRoleName)
                .anyMatch("ADMIN"::equals);

        if (!isAdmin && !booking.getBookedByUser().getUserId().equals(currentUser.getUserId())) {
            throw new IllegalArgumentException("Access denied: this booking does not belong to you");
        }

        return convertToDTO(
                booking
        );
    }


    // =========================================================
    // GET BOOKING BY REFERENCE
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public BookingDTO getBookingByReference(
            String bookingReference) {

        User currentUser = getAuthenticatedUser();

        Booking booking =
                bookingRepository
                        .findByBookingReferenceIgnoreCase(
                                bookingReference
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Booking with reference '"
                                                + bookingReference
                                                + "' not found"
                                )
                        );

        boolean isAdmin = currentUser.getUserRoles().stream()
                .map(UserRole::getRoleName)
                .anyMatch("ADMIN"::equals);

        if (!isAdmin && !booking.getBookedByUser().getUserId().equals(currentUser.getUserId())) {
            throw new IllegalArgumentException("Access denied: this booking does not belong to you");
        }

        return convertToDTO(
                booking
        );
    }


    // =========================================================
    // GET USER BOOKINGS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<BookingDTO> getBookingsByUser(
            String userId) {

        return bookingRepository
                .findByBookedByUser_UserId(
                        userId
                )
                .stream()
                .map(this::convertToDTO)
                .toList();
    }


    // =========================================================
    // GET CURRENT USER'S BOOKINGS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<BookingDTO> getMyBookings() {

        User user = getAuthenticatedUser();

        return bookingRepository
                .findByBookedByUser_UserId(
                        user.getUserId()
                )
                .stream()
                .map(this::convertToDTO)
                .toList();
    }


    // =========================================================
    // GET ALL BOOKINGS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<BookingDTO> getAllBookings() {
        User currentUser = getAuthenticatedUser();
        
        boolean isAdmin = currentUser.getUserRoles().stream()
                .map(UserRole::getRoleName)
                .anyMatch("ADMIN"::equals);

        if (isAdmin) {
            return bookingRepository
                    .findAll()
                    .stream()
                    .map(this::convertToDTO)
                    .toList();
        } else {
            // Role is BUS_OPERATOR or SUPPORT_AGENT
            if (currentUser.getOperator() == null) {
                return List.of(); // Return empty if no operator assigned
            }
            return bookingRepository
                    .findByTrip_Bus_Operator_OperatorId(currentUser.getOperator().getOperatorId())
                    .stream()
                    .map(this::convertToDTO)
                    .toList();
        }
    }


    // =========================================================
    // CANCEL BOOKING
    // =========================================================

    @Override
    public BookingDTO cancelBooking(
            String bookingId,
            BookingCancelRequest request) {

        User currentUser = getAuthenticatedUser();

        Booking booking =
                bookingRepository.findById(
                                bookingId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Booking not found"
                                )
                        );

        boolean isAdmin = currentUser.getUserRoles().stream()
                .map(UserRole::getRoleName)
                .anyMatch("ADMIN"::equals);

        boolean isSupportAgentForBooking = false;
        if (currentUser.getUserRoles().stream().anyMatch(r -> r.getRoleName().equals("SUPPORT_AGENT"))
                && currentUser.getOperator() != null) {
            String agentOperatorId = currentUser.getOperator().getOperatorId();
            if (booking.getTrip() != null && booking.getTrip().getBus() != null && booking.getTrip().getBus().getOperator() != null) {
                String bookingOperatorId = booking.getTrip().getBus().getOperator().getOperatorId();
                if (agentOperatorId.equals(bookingOperatorId)) {
                    isSupportAgentForBooking = true;
                }
            }
        }

        if (!isAdmin && !isSupportAgentForBooking && !booking.getBookedByUser().getUserId().equals(currentUser.getUserId())) {
            throw new IllegalArgumentException("Access denied: this booking does not belong to you, and you are not authorized to cancel it");
        }


        /*
         * Prevent duplicate cancellation.
         */
        if (booking.getBookingStatus()
                == BookingStatus.CANCELLED) {

            throw new IllegalArgumentException(
                    "Booking is already cancelled"
            );
        }


        /*
         * Only pending or confirmed bookings
         * can currently be cancelled.
         */
        if (booking.getBookingStatus()
                != BookingStatus.PENDING
                && booking.getBookingStatus()
                != BookingStatus.CONFIRMED) {

            throw new IllegalArgumentException(
                    "Booking cannot be cancelled in its current state"
            );
        }


        /*
         * A booking cannot be cancelled after
         * the scheduled trip departure.
         */
        Trip trip = booking.getTrip();

        LocalDateTime departureTime =
                LocalDateTime.of(
                        trip.getTravelDate(),
                        trip.getDepartureTime()
                );

        if (!departureTime.isAfter(
                LocalDateTime.now()
        )) {

            throw new IllegalArgumentException(
                    "Booking cannot be cancelled after departure"
            );
        }


        /*
         * Change booking status.
         */
        booking.setBookingStatus(
                BookingStatus.CANCELLED
        );


        /*
         * Cancellation reason will eventually
         * be stored through the Cancellation entity.
         *
         * Refund processing belongs to Payment/
         * Cancellation services.
         */

        return convertToDTO(
                bookingRepository.save(
                        booking
                )
        );
    }


    // =========================================================
    // BUSINESS LOGIC
    // =========================================================

    private void validateTripForBooking(
            Trip trip) {

        /*
         * Cancelled trip cannot be booked.
         */
        if (Boolean.TRUE.equals(
                trip.getIsCancelled()
        )) {

            throw new IllegalArgumentException(
                    "Cannot book a cancelled trip"
            );
        }


        /*
         * Trip must not have departed.
         */
        LocalDateTime departureTime =
                LocalDateTime.of(
                        trip.getTravelDate(),
                        trip.getDepartureTime()
                );

        if (!departureTime.isAfter(
                LocalDateTime.now()
        )) {

            throw new IllegalArgumentException(
                    "Cannot book a trip that has already departed"
            );
        }


        /*
         * Bus must be active.
         */
        if (trip.getBus() == null
                || !Boolean.TRUE.equals(
                trip.getBus().getIsActive()
        )) {

            throw new IllegalArgumentException(
                    "Bus assigned to this trip is inactive"
            );
        }


        /*
         * Route must be active.
         */
        if (trip.getRoute() == null
                || !Boolean.TRUE.equals(
                trip.getRoute().getIsActive()
        )) {

            throw new IllegalArgumentException(
                    "Route assigned to this trip is inactive"
            );
        }
    }


    private void validateRouteStops(
            Trip trip,
            RouteStop boardingPoint,
            RouteStop droppingPoint) {

        /*
         * Boarding point must belong to
         * the trip route.
         */
        if (!boardingPoint.getRoute()
                .getRouteId()
                .equals(
                        trip.getRoute()
                                .getRouteId()
                )) {

            throw new IllegalArgumentException(
                    "Boarding point does not belong to trip route"
            );
        }


        /*
         * Dropping point must belong to
         * the trip route.
         */
        if (!droppingPoint.getRoute()
                .getRouteId()
                .equals(
                        trip.getRoute()
                                .getRouteId()
                )) {

            throw new IllegalArgumentException(
                    "Dropping point does not belong to trip route"
            );
        }


        /*
         * Passenger cannot board after
         * the dropping location.
         *
         * After the stop-type refactoring, stopSequence is
         * scoped per StopType (BOARDING vs DROPPING) and
         * therefore cannot be compared across types.
         * Use distanceFromSourceKm which is route-global.
         */
        if (boardingPoint.getDistanceFromSourceKm()
                .compareTo(droppingPoint.getDistanceFromSourceKm()) >= 0) {

            throw new IllegalArgumentException(
                    "Boarding point must be before dropping point"
            );
        }
    }


    private void validateTripSeatForBooking(
            TripSeat tripSeat, User user) {

        /*
         * Seat must currently be available.
         */
        if (tripSeat.getSeatStatus()
                != SeatStatus.AVAILABLE) {

            throw new IllegalArgumentException(
                    "Selected seat is not available"
            );
        }


        /*
         * Physical bus seat must still be active.
         */
        if (tripSeat.getBusSeat() == null
                || !Boolean.TRUE.equals(
                tripSeat.getBusSeat().getIsActive()
        )) {

            throw new IllegalArgumentException(
                    "Selected physical seat is inactive"
            );
        }

        /*
         * Protect/Lock the seat for this booking.
         * Optimistic locking (@Version on TripSeat) ensures concurrency safety.
         */
        tripSeat.setSeatStatus(SeatStatus.TEMPORARILY_LOCKED);
        tripSeat.setLockedByUserId(user.getUserId());
        tripSeat.setLockExpiryTime(LocalDateTime.now().plusMinutes(10));
        tripSeatRepository.save(tripSeat);
    }


    private TripSeat findTripSeat(
            Trip trip,
            PassengerDTO passenger) {

        return tripSeatRepository
                .findByTrip_TripIdAndBusSeat_SeatNumber(
                        trip.getTripId(),
                        passenger.getSeatNumber()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Seat '"
                                        + passenger.getSeatNumber()
                                        + "' not found for this trip"
                        )
                );
    }


    private BookingSeat createBookingSeat(
            Booking booking,
            TripSeat tripSeat,
            PassengerDTO passenger) {

        BookingSeat bookingSeat =
                new BookingSeat();

        bookingSeat.setBookingSeatId(
                entityIdGenerator.generate(EntityIdGenerator.PREFIX_BOOKING_SEAT, bookingSeatId -> false)
        );

        bookingSeat.setBooking(
                booking
        );

        bookingSeat.setTripSeat(
                tripSeat
        );

        bookingSeat.setPassengerName(
                passenger.getPassengerName()
        );

        bookingSeat.setPassengerAge(
                passenger.getAge()
        );

        bookingSeat.setPassengerGender(
                passenger.getGender()
        );

        bookingSeat.setIdType(
                passenger.getIdType()
        );

        bookingSeat.setIdNumber(
                passenger.getIdNumber()
        );

        bookingSeat.setContactNumber(
                passenger.getContactNumber()
        );

        bookingSeat.setSeatFare(
                tripSeat.getSeatFare()
        );

        bookingSeat.setIsPrimary(
                Boolean.TRUE.equals(
                        passenger.getIsPrimary()
                )
        );

        return bookingSeat;
    }


    // =========================================================
    // DTO MAPPING
    // =========================================================

    private BookingDTO convertToDTO(
            Booking booking) {

        BookingDTO dto =
                new BookingDTO();

        dto.setBookingId(
                booking.getBookingId()
        );

        dto.setBookingReference(
                booking.getBookingReference()
        );

        dto.setBookingStatus(
                booking.getBookingStatus()
        );

        dto.setTotalSeats(
                booking.getTotalSeats()
        );

        dto.setBaseFareTotal(
                booking.getBaseFareTotal()
        );

        dto.setDiscountAmount(
                booking.getDiscountAmount()
        );

        dto.setTaxAmount(
                booking.getTaxAmount()
        );

        dto.setInsuranceAmount(
                booking.getInsuranceAmount()
        );

        dto.setTotalAmount(
                booking.getTotalAmount()
        );

        dto.setCreatedAt(
                booking.getCreatedAt()
        );


        if (booking.getBoardingPoint() != null) {

            dto.setBoardingPointName(
                    booking.getBoardingPoint()
                            .getStopName()
            );
        }


        if (booking.getDroppingPoint() != null) {

            dto.setDroppingPointName(
                    booking.getDroppingPoint()
                            .getStopName()
            );
        }


        if (booking.getBookedByUser() != null) {

            dto.setUserId(
                    booking.getBookedByUser()
                            .getUserId()
            );
            
            dto.setUserName(
                    booking.getBookedByUser()
                            .getUserName()
            );
            
            dto.setUserEmail(
                    booking.getBookedByUser()
                            .getUserEmail()
            );
        }


        if (booking.getTrip() != null) {

            dto.setTripId(
                    booking.getTrip()
                            .getTripId()
            );
        }


        if (booking.getBookingSeats() != null) {

            dto.setBookingSeats(
                    booking.getBookingSeats()
                            .stream()
                            .map(
                                    this::convertToBookingSeatDTO
                            )
                            .toList()
            );
        }

        return dto;
    }


    private BookingSeatDTO convertToBookingSeatDTO(
            BookingSeat bookingSeat) {

        BookingSeatDTO dto =
                new BookingSeatDTO();

        dto.setBookingSeatId(
                bookingSeat.getBookingSeatId()
        );

        dto.setPassengerName(
                bookingSeat.getPassengerName()
        );

        dto.setPassengerAge(
                bookingSeat.getPassengerAge()
        );

        dto.setPassengerGender(
                bookingSeat.getPassengerGender()
        );

        dto.setIdType(
                bookingSeat.getIdType()
        );

        dto.setIdNumber(
                bookingSeat.getIdNumber()
        );

        dto.setContactNumber(
                bookingSeat.getContactNumber()
        );

        dto.setSeatFare(
                bookingSeat.getSeatFare()
        );

        dto.setIsPrimary(
                bookingSeat.getIsPrimary()
        );


        if (bookingSeat.getTripSeat() != null
                && bookingSeat.getTripSeat()
                .getBusSeat() != null) {

            dto.setSeatNumber(
                    bookingSeat.getTripSeat()
                            .getBusSeat()
                            .getSeatNumber()
            );
        }

        return dto;
    }


    // =========================================================
    // BOOKING REFERENCE
    // =========================================================

    private String generateBookingReference() {
        return entityIdGenerator.generate(
                EntityIdGenerator.PREFIX_BOOKING,
                ref -> bookingRepository.findByBookingReferenceIgnoreCase(ref).isPresent()
        );
    }


    // =========================================================
    // AUTHENTICATED USER
    // =========================================================

    private User getAuthenticatedUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "User is not authenticated"
            );
        }

        String username =
                authentication.getName();

        return userRepository
                .findByUserEmail(username)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Authenticated user not found"
                        )
                );
    }
}