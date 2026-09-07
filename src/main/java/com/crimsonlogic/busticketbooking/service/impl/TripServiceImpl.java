package com.crimsonlogic.busticketbooking.service.impl;

import com.crimsonlogic.busticketbooking.dto.SeatLayoutDTO;
import com.crimsonlogic.busticketbooking.dto.TripCreateRequest;
import com.crimsonlogic.busticketbooking.dto.TripDTO;
import com.crimsonlogic.busticketbooking.dto.TripSearchRequest;
import com.crimsonlogic.busticketbooking.dto.TripSeatDTO;
import com.crimsonlogic.busticketbooking.entity.Bus;
import com.crimsonlogic.busticketbooking.entity.Route;
import com.crimsonlogic.busticketbooking.entity.Trip;
import com.crimsonlogic.busticketbooking.entity.TripSeat;
import com.crimsonlogic.busticketbooking.enums.BookingStatus;
import com.crimsonlogic.busticketbooking.enums.SeatStatus;
import com.crimsonlogic.busticketbooking.repository.BookingRepository;
import com.crimsonlogic.busticketbooking.repository.BusRepository;
import com.crimsonlogic.busticketbooking.repository.RouteRepository;
import com.crimsonlogic.busticketbooking.repository.TripRepository;
import com.crimsonlogic.busticketbooking.repository.TripSeatRepository;
import com.crimsonlogic.busticketbooking.repository.UserRepository;
import com.crimsonlogic.busticketbooking.service.TripService;
import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final BusRepository busRepository;
    private final RouteRepository routeRepository;
    private final com.crimsonlogic.busticketbooking.repository.BusSeatRepository busSeatRepository;
    private final TripSeatRepository tripSeatRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final com.crimsonlogic.busticketbooking.repository.RouteStopRepository routeStopRepository;
    private final com.crimsonlogic.busticketbooking.repository.TripStopFareRepository tripStopFareRepository;

    /** Booking statuses that constitute an "active" booking. */
    private static final java.util.List<BookingStatus> ACTIVE_STATUSES =
            java.util.List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING);

    /**
     * Verifies that the currently authenticated BUS_OPERATOR owns the given bus.
     * ADMINs bypass this check.
     */
    private void authorizeOperatorForBus(Bus bus) {
        String email = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        com.crimsonlogic.busticketbooking.entity.User user = userRepository
                .findByUserEmailIgnoreCase(email)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("User not found"));

        boolean isAdmin = user.getUserRoles().stream()
                .anyMatch(r -> "ADMIN".equals(r.getRoleName()));
        if (isAdmin) return;

        if (user.getOperator() == null || bus.getOperator() == null
                || !user.getOperator().getOperatorId().equals(bus.getOperator().getOperatorId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not authorized to manage trips for this bus.");
        }
    }

    @Override
    public TripDTO createTrip(
            TripCreateRequest request) {

        validateTripRequest(request);

        Bus bus = findBus(
                request.getBusRegistrationNumber()
        );

        authorizeOperatorForBus(bus);

        if (!Boolean.TRUE.equals(bus.getIsActive())) {
            throw new IllegalArgumentException(
                    "Cannot create a trip using an inactive bus"
            );
        }

        Route route = findRoute(
                request.getSource(),
                request.getDestination()
        );

        if (!Boolean.TRUE.equals(route.getIsActive())) {
            throw new IllegalArgumentException(
                    "Cannot create a trip using an inactive route"
            );
        }

        // ── Scheduling conflict check ──────────────────────────────
        // A bus can only operate one route per date.
        boolean hasConflict = tripRepository
                .findByBus_BusIdAndTravelDate(bus.getBusId(), request.getTravelDate())
                .stream()
                .anyMatch(existing -> !Boolean.TRUE.equals(existing.getIsCancelled()));

        if (hasConflict) {
            throw new IllegalArgumentException(
                    "Bus '" + request.getBusRegistrationNumber()
                            + "' is already scheduled on "
                            + request.getTravelDate()
                            + ". A bus can only operate one trip per date."
            );
        }
        // ──────────────────────────────────────────────────────────

        Trip trip = new Trip();

        trip.setTripId(
                generateId()
        );

        trip.setTravelDate(
                request.getTravelDate()
        );

        trip.setDepartureTime(
                request.getDepartureTime()
        );

        trip.setArrivalTime(
                request.getArrivalTime()
        );

        trip.setArrivalDate(
                request.getArrivalDate()
        );

        trip.setBaseFare(
                request.getBaseFare()
        );

        trip.setIsCancelled(false);

        trip.setBus(bus);
        trip.setRoute(route);

        Trip savedTrip =
                tripRepository.save(trip);

        /*
         * Create TripSeat inventory based on the physical BusSeat layout.
         */
        List<com.crimsonlogic.busticketbooking.entity.BusSeat> busSeats = 
                busSeatRepository.findByBus_RegistrationNumberIgnoreCase(bus.getRegistrationNumber());

        List<TripSeat> tripSeats = busSeats.stream().map(busSeat -> {
            TripSeat ts = new TripSeat();
            ts.setSeatStatus(SeatStatus.AVAILABLE);
            ts.setSeatFare(savedTrip.getBaseFare());
            ts.setTrip(savedTrip);
            ts.setBusSeat(busSeat);
            return ts;
        }).toList();

        tripSeatRepository.saveAll(tripSeats);

        // Save TripStopFares if provided
        if (request.getStopFares() != null && !request.getStopFares().isEmpty()) {
            java.util.List<com.crimsonlogic.busticketbooking.entity.TripStopFare> faresToSave = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, java.math.BigDecimal> entry : request.getStopFares().entrySet()) {
                com.crimsonlogic.busticketbooking.entity.RouteStop routeStop = routeStopRepository.findById(entry.getKey()).orElse(null);
                if (routeStop != null && routeStop.getRoute().getRouteId().equals(route.getRouteId())) {
                    com.crimsonlogic.busticketbooking.entity.TripStopFare tsf = new com.crimsonlogic.busticketbooking.entity.TripStopFare();
                    tsf.setTrip(savedTrip);
                    tsf.setRouteStop(routeStop);
                    tsf.setFareFromSource(entry.getValue());
                    faresToSave.add(tsf);
                }
            }
            if (!faresToSave.isEmpty()) {
                tripStopFareRepository.saveAll(faresToSave);
                savedTrip.setStopFares(faresToSave);
            }
        }

        return convertToDTO(savedTrip);
    }

    @Override
    public TripDTO updateTrip(
            String busRegistrationNumber,
            String source,
            String destination,
            LocalDate travelDate,
            TripCreateRequest request) {

        Trip trip = findTrip(
                busRegistrationNumber,
                source,
                destination,
                travelDate
        );

        // Verify the operator owns this trip's bus
        authorizeOperatorForBus(trip.getBus());

        if (Boolean.TRUE.equals(trip.getIsCancelled())) {
            throw new IllegalArgumentException(
                    "Cancelled trip cannot be updated"
            );
        }

        LocalDateTime departure =
                LocalDateTime.of(
                        trip.getTravelDate(),
                        trip.getDepartureTime()
                );

        if (!departure.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Past or ongoing trip cannot be updated"
            );
        }

        // ── Active-booking protection (Phase 6) ───────────────────
        // Block any modification to a trip that has CONFIRMED or PENDING
        // bookings. Passengers must cancel their bookings first,
        // or the trip must have already concluded.
        long activeBookings = bookingRepository
                .countByTrip_TripIdAndBookingStatusIn(
                        trip.getTripId(),
                        ACTIVE_STATUSES
                );

        if (activeBookings > 0) {
            throw new IllegalArgumentException(
                    "Cannot modify trip: "
                            + activeBookings
                            + " active booking(s) exist. "
                            + "The trip is protected until all bookings "
                            + "are cancelled or the trip ends."
            );
        }
        // ──────────────────────────────────────────────────────────

        validateTripRequest(request);

        Bus bus = findBus(
                request.getBusRegistrationNumber()
        );

        if (!Boolean.TRUE.equals(bus.getIsActive())) {
            throw new IllegalArgumentException(
                    "Cannot assign an inactive bus"
            );
        }

        Route route = findRoute(
                request.getSource(),
                request.getDestination()
        );

        if (!Boolean.TRUE.equals(route.getIsActive())) {
            throw new IllegalArgumentException(
                    "Cannot assign an inactive route"
            );
        }

        // ── Scheduling conflict check ──────────────────────────────
        // Ensure the new bus/date combination does not already have
        // another active trip (excluding the trip currently being updated).
        final String currentTripId = trip.getTripId();
        boolean hasConflict = tripRepository
                .findByBus_BusIdAndTravelDate(bus.getBusId(), request.getTravelDate())
                .stream()
                .filter(existing -> !existing.getTripId().equals(currentTripId))
                .anyMatch(existing -> !Boolean.TRUE.equals(existing.getIsCancelled()));

        if (hasConflict) {
            throw new IllegalArgumentException(
                    "Bus '" + request.getBusRegistrationNumber()
                            + "' is already scheduled on "
                            + request.getTravelDate()
                            + ". A bus can only operate one trip per date."
            );
        }
        // ──────────────────────────────────────────────────────────

        trip.setBus(bus);
        trip.setRoute(route);

        trip.setTravelDate(
                request.getTravelDate()
        );

        trip.setDepartureTime(
                request.getDepartureTime()
        );

        trip.setArrivalTime(
                request.getArrivalTime()
        );

        trip.setArrivalDate(
                request.getArrivalDate()
        );

        trip.setBaseFare(
                request.getBaseFare()
        );

        Trip savedTrip = tripRepository.save(trip);

        // Update TripStopFares if provided
        if (request.getStopFares() != null) {
            // Delete existing fares first (simplified update)
            tripStopFareRepository.deleteAll(tripStopFareRepository.findByTrip_TripId(savedTrip.getTripId()));
            
            java.util.List<com.crimsonlogic.busticketbooking.entity.TripStopFare> faresToSave = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, java.math.BigDecimal> entry : request.getStopFares().entrySet()) {
                com.crimsonlogic.busticketbooking.entity.RouteStop routeStop = routeStopRepository.findById(entry.getKey()).orElse(null);
                if (routeStop != null && routeStop.getRoute().getRouteId().equals(route.getRouteId())) {
                    com.crimsonlogic.busticketbooking.entity.TripStopFare tsf = new com.crimsonlogic.busticketbooking.entity.TripStopFare();
                    tsf.setTrip(savedTrip);
                    tsf.setRouteStop(routeStop);
                    tsf.setFareFromSource(entry.getValue());
                    faresToSave.add(tsf);
                }
            }
            if (!faresToSave.isEmpty()) {
                tripStopFareRepository.saveAll(faresToSave);
                savedTrip.setStopFares(faresToSave);
            }
        }

        return convertToDTO(savedTrip);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripDTO> searchTrips(
            TripSearchRequest request) {

        List<Trip> trips;

        if (request.getTravelDate() != null) {
            trips = tripRepository
                    .findByIntermediateStopsAndTravelDate(
                            request.getSource(),
                            request.getDestination(),
                            request.getTravelDate()
                    );
        } else {
            trips = tripRepository
                    .findByIntermediateStops(
                            request.getSource(),
                            request.getDestination()
                    );
        }

        return trips.stream()

                // Do not show cancelled trips
                .filter(trip ->
                        !Boolean.TRUE.equals(
                                trip.getIsCancelled()
                        )
                )

                // Do not show trips that have already departed
                .filter(trip -> 
                        java.time.LocalDateTime.of(trip.getTravelDate(), trip.getDepartureTime())
                                .isAfter(java.time.LocalDateTime.now())
                )

                // Bus type filter
                .filter(trip ->
                        request.getBusType() == null
                                || (
                                trip.getBus() != null
                                        && trip.getBus()
                                        .getBusType()
                                        .equals(
                                                request.getBusType()
                                        )
                        )
                )

                // Minimum price filter
                .filter(trip ->
                        request.getMinPrice() == null
                                || (
                                trip.getBaseFare() != null
                                        && trip.getBaseFare()
                                        .compareTo(
                                                request.getMinPrice()
                                        ) >= 0
                        )
                )

                // Maximum price filter
                .filter(trip ->
                        request.getMaxPrice() == null
                                || (
                                trip.getBaseFare() != null
                                        && trip.getBaseFare()
                                        .compareTo(
                                                request.getMaxPrice()
                                        ) <= 0
                        )
                )

                // Departure start filter
                .filter(trip ->
                        request.getDepartureStart() == null
                                || (
                                trip.getDepartureTime() != null
                                        && !trip.getDepartureTime()
                                        .isBefore(
                                                request.getDepartureStart()
                                        )
                        )
                )

                // Departure end filter
                .filter(trip ->
                        request.getDepartureEnd() == null
                                || (
                                trip.getDepartureTime() != null
                                        && !trip.getDepartureTime()
                                        .isAfter(
                                                request.getDepartureEnd()
                                        )
                        )
                )

                // AC filter
                .filter(trip ->
                        request.getIsAc() == null
                                || (
                                trip.getBus() != null
                                        && request.getIsAc().equals(
                                                trip.getBus().getAmenities() != null 
                                                    && trip.getBus().getAmenities().contains("AC")
                                        )
                        )
                )

                .map(trip -> convertToDTOWithDynamicFare(trip, request.getSource(), request.getDestination()))
                .toList();
    }

    private TripDTO convertToDTOWithDynamicFare(Trip trip, String searchSource, String searchDestination) {
        TripDTO dto = convertToDTO(trip);
        
        // If exact source and destination match the route, baseFare is unchanged
        if (trip.getRoute().getSource().equalsIgnoreCase(searchSource) && 
            trip.getRoute().getDestination().equalsIgnoreCase(searchDestination)) {
            return dto;
        }

        // Otherwise, calculate dynamic fare based on TripStopFares
        if (trip.getStopFares() != null && !trip.getStopFares().isEmpty()) {
            java.math.BigDecimal sourceFare = java.math.BigDecimal.ZERO;
            java.math.BigDecimal destFare = trip.getBaseFare();

            for (com.crimsonlogic.busticketbooking.entity.TripStopFare tsf : trip.getStopFares()) {
                if (tsf.getRouteStop().getStopName().equalsIgnoreCase(searchSource)) {
                    sourceFare = tsf.getFareFromSource();
                }
                if (tsf.getRouteStop().getStopName().equalsIgnoreCase(searchDestination)) {
                    destFare = tsf.getFareFromSource();
                }
            }

            java.math.BigDecimal dynamicFare = destFare.subtract(sourceFare);
            if (dynamicFare.compareTo(java.math.BigDecimal.ZERO) > 0) {
                dto.setBaseFare(dynamicFare);
            }
        }
        
        return dto;
    }


    @Override
    @Transactional(readOnly = true)
    public List<TripDTO> getAllTrips() {

        return tripRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripDTO> getOperatorTrips() {
        String email = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        com.crimsonlogic.busticketbooking.entity.User user = userRepository
                .findByUserEmailIgnoreCase(email)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("User not found"));

        if (user.getOperator() == null) {
            return java.util.List.of();
        }

        return tripRepository
                .findByBus_Operator_OperatorId(user.getOperator().getOperatorId())
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public void cancelTrip(
            String busRegistrationNumber,
            String source,
            String destination,
            LocalDate travelDate,
            String reason) {

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Cancellation reason is required"
            );
        }

        Trip trip = findTrip(
                busRegistrationNumber,
                source,
                destination,
                travelDate
        );

        // Verify the operator owns this trip's bus
        authorizeOperatorForBus(trip.getBus());

        if (Boolean.TRUE.equals(trip.getIsCancelled())) {
            throw new IllegalArgumentException(
                    "Trip is already cancelled"
            );
        }

        // ── Active-booking protection (Phase 6) ───────────────────
        // A trip with live bookings cannot be outright cancelled.
        // Doing so would invalidate passenger bookings without warning.
        long activeBookings = bookingRepository
                .countByTrip_TripIdAndBookingStatusIn(
                        trip.getTripId(),
                        ACTIVE_STATUSES
                );

        if (activeBookings > 0) {
            throw new IllegalArgumentException(
                    "Cannot cancel trip: "
                            + activeBookings
                            + " active booking(s) exist. "
                            + "Passengers must cancel their bookings first, "
                            + "or wait for the trip to conclude."
            );
        }
        // ──────────────────────────────────────────────────────────

        trip.setIsCancelled(true);
        trip.setCancellationReason(
                reason.trim()
        );

        tripRepository.save(trip);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripSeatDTO> getTripSeats(
            String busRegistrationNumber,
            String source,
            String destination,
            LocalDate travelDate) {

        Trip trip = findTrip(
                busRegistrationNumber,
                source,
                destination,
                travelDate
        );

        return tripSeatRepository
                .findByTrip_TripIdOrderByBusSeat_SeatNumberAsc(
                        trip.getTripId()
                )
                .stream()
                .map(this::convertToTripSeatDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripSeatDTO> getAvailableSeats(
            String busRegistrationNumber,
            String source,
            String destination,
            LocalDate travelDate) {

        Trip trip = findTrip(
                busRegistrationNumber,
                source,
                destination,
                travelDate
        );

        if (Boolean.TRUE.equals(trip.getIsCancelled())) {
            throw new IllegalArgumentException(
                    "Seats are not available for a cancelled trip"
            );
        }

        return tripSeatRepository
                .findByTrip_TripIdAndSeatStatus(
                        trip.getTripId(),
                        SeatStatus.AVAILABLE
                )
                .stream()
                .map(this::convertToTripSeatDTO)
                .toList();
    }

    private Bus findBus(
            String registrationNumber) {

        return busRepository
                .findByRegistrationNumberIgnoreCase(
                        registrationNumber
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Bus with registration number '"
                                        + registrationNumber
                                        + "' not found"
                        )
                );
    }

    private Route findRoute(
            String source,
            String destination) {

        return routeRepository
                .findBySourceIgnoreCaseAndDestinationIgnoreCase(
                        source,
                        destination
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Route from '"
                                        + source
                                        + "' to '"
                                        + destination
                                        + "' not found"
                        )
                );
    }

    private Trip findTrip(
            String busRegistrationNumber,
            String source,
            String destination,
            LocalDate travelDate) {

        return tripRepository
                .findByIntermediateStopsAndTravelDate(
                        source,
                        destination,
                        travelDate
                )
                .stream()
                .filter(trip ->
                        trip.getBus()
                                .getRegistrationNumber()
                                .equalsIgnoreCase(
                                        busRegistrationNumber
                                )
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Trip not found"
                        )
                );
    }

    private void validateTripRequest(
            TripCreateRequest request) {

        if (request.getTravelDate()
                .isBefore(LocalDate.now())) {

            throw new IllegalArgumentException(
                    "Travel date cannot be in the past"
            );
        }

        if (request.getArrivalDate().isBefore(request.getTravelDate())) {
            throw new IllegalArgumentException(
                    "Arrival date cannot be before travel date"
            );
        }

        if (request.getArrivalDate().isEqual(request.getTravelDate()) && 
            !request.getArrivalTime().isAfter(request.getDepartureTime())) {
            
            throw new IllegalArgumentException(
                    "Arrival time must be after departure time if arriving on the same day"
            );
        }
    }

    private boolean matchesSearchCriteria(
            Trip trip,
            TripSearchRequest request) {

        if (request.getBusType() != null
                && trip.getBus().getBusType()
                != request.getBusType()) {

            return false;
        }

        if (request.getMinPrice() != null
                && trip.getBaseFare()
                .compareTo(request.getMinPrice()) < 0) {

            return false;
        }

        if (request.getMaxPrice() != null
                && trip.getBaseFare()
                .compareTo(request.getMaxPrice()) > 0) {

            return false;
        }

        if (request.getDepartureStart() != null
                && trip.getDepartureTime()
                .isBefore(request.getDepartureStart())) {

            return false;
        }

        if (request.getDepartureEnd() != null
                && trip.getDepartureTime()
                .isAfter(request.getDepartureEnd())) {

            return false;
        }

        return true;
    }

    private TripDTO convertToDTO(
            Trip trip) {

        TripDTO dto = new TripDTO();

        dto.setTripId(
                trip.getTripId()
        );

        dto.setTravelDate(
                trip.getTravelDate()
        );

        dto.setDepartureTime(
                trip.getDepartureTime()
        );

        dto.setArrivalTime(
                trip.getArrivalTime()
        );

        dto.setArrivalDate(
                trip.getArrivalDate()
        );

        dto.setBaseFare(
                trip.getBaseFare()
        );

        dto.setIsCancelled(
                trip.getIsCancelled()
        );

        dto.setCancellationReason(
                trip.getCancellationReason()
        );

        if (trip.getBus() != null) {
            Bus bus = trip.getBus();

            dto.setBusRegistrationNumber(
                    bus.getRegistrationNumber()
            );

            dto.setBusType(
                    bus.getBusType() != null
                            ? bus.getBusType().name()
                            : null
            );

            dto.setAmenities(
                    bus.getAmenities()
            );

            if (bus.getOperator() != null) {
                dto.setOperatorName(
                        bus.getOperator().getCompanyName()
                );
            }
        }

        if (trip.getRoute() != null) {
            dto.setSource(
                    trip.getRoute().getSource()
            );

            dto.setDestination(
                    trip.getRoute().getDestination()
            );
        }

        // Compute seat availability from TripSeats already loaded
        if (trip.getTripSeats() != null) {
            int total = trip.getTripSeats().size();
            long available = trip.getTripSeats().stream()
                    .filter(ts -> com.crimsonlogic.busticketbooking.enums.SeatStatus.AVAILABLE
                            .equals(ts.getSeatStatus()))
                    .count();
            dto.setTotalSeats(total);
            dto.setAvailableSeats((int) available);
        }

        if (trip.getStopFares() != null && !trip.getStopFares().isEmpty()) {
            dto.setStopFares(trip.getStopFares().stream().map(tsf -> {
                com.crimsonlogic.busticketbooking.dto.TripStopFareDTO tsfDTO = new com.crimsonlogic.busticketbooking.dto.TripStopFareDTO();
                tsfDTO.setRouteStopId(tsf.getRouteStop().getRouteStopId());
                tsfDTO.setStopName(tsf.getRouteStop().getStopName());
                tsfDTO.setFareFromSource(tsf.getFareFromSource());
                tsfDTO.setStopSequence(tsf.getRouteStop().getStopSequence());
                tsfDTO.setStopType(tsf.getRouteStop().getStopType() != null ? tsf.getRouteStop().getStopType().name() : null);
                return tsfDTO;
            }).toList());
        } else {
            dto.setStopFares(new java.util.ArrayList<>());
        }

        return dto;
    }


    private TripSeatDTO convertToTripSeatDTO(
            TripSeat tripSeat) {

        TripSeatDTO dto =
                new TripSeatDTO();

        dto.setTripSeatId(
                tripSeat.getTripSeatId()
        );

        dto.setSeatStatus(
                tripSeat.getSeatStatus()
        );

        dto.setSeatFare(
                tripSeat.getSeatFare()
        );

        dto.setLockExpiryTime(
                tripSeat.getLockExpiryTime()
        );

        if (tripSeat.getBusSeat() != null) {

            dto.setSeatNumber(
                    tripSeat.getBusSeat()
                            .getSeatNumber()
            );

            dto.setSeatPosition(
                    tripSeat.getBusSeat()
                            .getSeatPosition()
            );
        }

        return dto;
    }

    private String generateId() {
        return EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_TRIP);
    }

    @Override
    @Transactional(readOnly = true)
    public TripDTO getTripById(String tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
        return convertToDTO(trip);
    }

    @Override
    @Transactional(readOnly = true)
    public SeatLayoutDTO getSeatLayout(String tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        List<TripSeat> seats = tripSeatRepository
                .findByTrip_TripIdOrderByBusSeat_SeatNumberAsc(tripId);

        List<TripSeatDTO> lower = seats.stream()
                .filter(s -> s.getBusSeat() != null
                        && s.getBusSeat().getSeatPosition()
                           == com.crimsonlogic.busticketbooking.enums.SeatPosition.LOWER)
                .map(this::convertToTripSeatDTO)
                .toList();

        List<TripSeatDTO> upper = seats.stream()
                .filter(s -> s.getBusSeat() != null
                        && s.getBusSeat().getSeatPosition()
                           == com.crimsonlogic.busticketbooking.enums.SeatPosition.UPPER)
                .map(this::convertToTripSeatDTO)
                .toList();

        long available = seats.stream()
                .filter(s -> s.getSeatStatus() == SeatStatus.AVAILABLE)
                .count();

        SeatLayoutDTO layout = new SeatLayoutDTO();
        layout.setTripId(tripId);
        layout.setLowerDeck(lower);
        layout.setUpperDeck(upper);
        layout.setTotalSeats(seats.size());
        layout.setAvailableSeats((int) available);

        if (trip.getBus() != null) {
            layout.setBusType(trip.getBus().getBusType() != null
                    ? trip.getBus().getBusType().name() : "");
            if (trip.getBus().getOperator() != null) {
                layout.setOperatorName(trip.getBus().getOperator().getCompanyName());
            }
        }
        if (trip.getRoute() != null) {
            layout.setSource(trip.getRoute().getSource());
            layout.setDestination(trip.getRoute().getDestination());
        }

        return layout;
    }

    /**
     * Back-fills TripSeat rows for existing trips that currently have no seats.
     * Useful after the seat auto-generation feature was introduced.
     * @return count of trips that were back-filled
     */
    @Override
    public int backfillTripSeats() {
        List<Trip> allTrips = tripRepository.findAll();
        int count = 0;
        for (Trip trip : allTrips) {
            long existing = tripSeatRepository.countByTrip_TripId(trip.getTripId());
            if (existing == 0 && trip.getBus() != null) {
                List<com.crimsonlogic.busticketbooking.entity.BusSeat> busSeats =
                        busSeatRepository.findByBus_RegistrationNumberIgnoreCase(
                                trip.getBus().getRegistrationNumber());
                if (!busSeats.isEmpty()) {
                    List<TripSeat> tripSeats = busSeats.stream().map(busSeat -> {
                        TripSeat ts = new TripSeat();
                        ts.setSeatStatus(SeatStatus.AVAILABLE);
                        ts.setSeatFare(trip.getBaseFare());
                        ts.setTrip(trip);
                        ts.setBusSeat(busSeat);
                        return ts;
                    }).toList();
                    tripSeatRepository.saveAll(tripSeats);
                    count++;
                }
            }
        }
        return count;
    }
}