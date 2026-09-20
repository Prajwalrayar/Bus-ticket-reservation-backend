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
    private final com.crimsonlogic.busticketbooking.repository.TripSegmentRepository tripSegmentRepository;
    private final com.crimsonlogic.busticketbooking.service.LocationService locationService;

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

        if (request != null && request.getSegments() != null) {
            validateSegmentsTiming(request.getSegments());
        }

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

        // Auto-derive departure/arrival from stopTimes if not explicitly provided
        java.util.List<com.crimsonlogic.busticketbooking.entity.RouteStop> orderedStops =
                route.getRouteStops().stream()
                        .sorted(java.util.Comparator.comparingInt(
                                com.crimsonlogic.busticketbooking.entity.RouteStop::getStopSequence))
                        .toList();

        java.time.LocalTime derivedDeparture = request.getDepartureTime();
        java.time.LocalTime derivedArrival = request.getArrivalTime();
        java.time.LocalDate derivedArrivalDate = request.getArrivalDate();

        if (request.getSegments() != null && !request.getSegments().isEmpty()) {
            if (derivedDeparture == null) {
                com.crimsonlogic.busticketbooking.dto.TripSegmentCreateRequest firstSeg = request.getSegments().stream()
                        .min(java.util.Comparator.comparingDouble(seg -> {
                            return route.getRouteStops().stream()
                                    .filter(rs -> rs.getRouteStopId().equals(seg.getBoardingStopId()))
                                    .findFirst()
                                    .map(rs -> rs.getDistanceFromSourceKm().doubleValue())
                                    .orElse(Double.MAX_VALUE);
                        }))
                        .orElse(request.getSegments().get(0));
                derivedDeparture = firstSeg.getDepartureTime();
            }
            if (derivedArrival == null || derivedArrivalDate == null) {
                com.crimsonlogic.busticketbooking.dto.TripSegmentCreateRequest lastSeg = request.getSegments().stream()
                        .max(java.util.Comparator.comparingDouble(seg -> {
                            return route.getRouteStops().stream()
                                    .filter(rs -> rs.getRouteStopId().equals(seg.getDroppingStopId()))
                                    .findFirst()
                                    .map(rs -> rs.getDistanceFromSourceKm().doubleValue())
                                    .orElse(Double.MIN_VALUE);
                        }))
                        .orElse(request.getSegments().get(request.getSegments().size() - 1));
                
                if (derivedArrival == null) {
                    derivedArrival = lastSeg.getArrivalTime();
                }
                if (derivedArrivalDate == null) {
                    derivedArrivalDate = lastSeg.getArrivalDate();
                }
            }
        }

        trip.setDepartureTime(derivedDeparture);
        trip.setArrivalTime(derivedArrival);
        trip.setArrivalDate(derivedArrivalDate != null ? derivedArrivalDate : request.getTravelDate());

        trip.setBaseFare(
                request.getBaseFare()
        );

        trip.setIsCancelled(false);

        trip.setBus(bus);
        trip.setRoute(route);

        Trip savedTrip =
                tripRepository.save(trip);

        // Update the bus's last trip date so the auto-deactivation scheduler
        // can correctly detect inactivity.
        if (bus.getLastTripDate() == null || request.getTravelDate().isAfter(bus.getLastTripDate())) {
            bus.setLastTripDate(request.getTravelDate());
            busRepository.save(bus);
        }

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

        // Save TripSegments
        java.util.List<com.crimsonlogic.busticketbooking.entity.TripSegment> segmentsToSave = new java.util.ArrayList<>();
        if (request.getSegments() != null) {
            for (com.crimsonlogic.busticketbooking.dto.TripSegmentCreateRequest segReq : request.getSegments()) {
                com.crimsonlogic.busticketbooking.entity.RouteStop bStop = routeStopRepository.findById(segReq.getBoardingStopId())
                        .orElseThrow(() -> new IllegalArgumentException("Boarding stop not found"));
                com.crimsonlogic.busticketbooking.entity.RouteStop dStop = routeStopRepository.findById(segReq.getDroppingStopId())
                        .orElseThrow(() -> new IllegalArgumentException("Dropping stop not found"));

                com.crimsonlogic.busticketbooking.entity.TripSegment ts = new com.crimsonlogic.busticketbooking.entity.TripSegment();
                ts.setTrip(savedTrip);
                ts.setBoardingStop(bStop);
                ts.setDroppingStop(dStop);
                ts.setDepartureTime(segReq.getDepartureTime());
                ts.setArrivalTime(segReq.getArrivalTime());
                ts.setDepartureDate(segReq.getDepartureDate() != null ? segReq.getDepartureDate() : savedTrip.getTravelDate());
                ts.setArrivalDate(segReq.getArrivalDate() != null ? segReq.getArrivalDate() : savedTrip.getTravelDate());
                ts.setFare(segReq.getFare());
                segmentsToSave.add(ts);
            }
        }

        if (!segmentsToSave.isEmpty()) {
            tripSegmentRepository.saveAll(segmentsToSave);
            savedTrip.setTripSegments(segmentsToSave);
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

        if (request != null && request.getSegments() != null) {
            validateSegmentsTiming(request.getSegments());
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
                    "Cancelled trip cannot be updated"
            );
        }

        LocalDateTime departure =
                LocalDateTime.of(
                        trip.getTravelDate(),
                        trip.getDepartureTime()
                );

        if (LocalDateTime.now().plusHours(5).isAfter(departure)) {
            throw new IllegalArgumentException(
                    "Trip cannot be updated within 5 hours of departure"
            );
        }
        
        if (!trip.getBus().getRegistrationNumber().equalsIgnoreCase(request.getBusRegistrationNumber()) ||
            !trip.getRoute().getSource().equalsIgnoreCase(request.getSource()) ||
            !trip.getRoute().getDestination().equalsIgnoreCase(request.getDestination()) ||
            !trip.getTravelDate().equals(request.getTravelDate())) {
            throw new IllegalArgumentException(
                    "Cannot modify core trip details (Bus, Route, or Date) during an update."
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

        java.util.List<com.crimsonlogic.busticketbooking.entity.TripSegment> existingSegments = trip.getTripSegments();
        if (existingSegments == null) {
            existingSegments = new java.util.ArrayList<>();
            trip.setTripSegments(existingSegments);
        }

        java.util.List<com.crimsonlogic.busticketbooking.entity.TripSegment> retainedSegments = new java.util.ArrayList<>();

        if (request.getSegments() != null) {
            for (com.crimsonlogic.busticketbooking.dto.TripSegmentCreateRequest segReq : request.getSegments()) {
                com.crimsonlogic.busticketbooking.entity.RouteStop bStop = routeStopRepository.findById(segReq.getBoardingStopId())
                        .orElseThrow(() -> new IllegalArgumentException("Boarding stop not found"));
                com.crimsonlogic.busticketbooking.entity.RouteStop dStop = routeStopRepository.findById(segReq.getDroppingStopId())
                        .orElseThrow(() -> new IllegalArgumentException("Dropping stop not found"));

                com.crimsonlogic.busticketbooking.entity.TripSegment existingTs = null;
                for (com.crimsonlogic.busticketbooking.entity.TripSegment ts : existingSegments) {
                    if (ts.getBoardingStop().getRouteStopId().equals(bStop.getRouteStopId()) &&
                        ts.getDroppingStop().getRouteStopId().equals(dStop.getRouteStopId())) {
                        existingTs = ts;
                        break;
                    }
                }

                if (existingTs != null) {
                    existingTs.setDepartureTime(segReq.getDepartureTime());
                    existingTs.setArrivalTime(segReq.getArrivalTime());
                    existingTs.setDepartureDate(segReq.getDepartureDate() != null ? segReq.getDepartureDate() : trip.getTravelDate());
                    existingTs.setArrivalDate(segReq.getArrivalDate() != null ? segReq.getArrivalDate() : trip.getTravelDate());
                    existingTs.setFare(segReq.getFare());
                    retainedSegments.add(existingTs);
                } else {
                    com.crimsonlogic.busticketbooking.entity.TripSegment ts = new com.crimsonlogic.busticketbooking.entity.TripSegment();
                    ts.setTrip(trip);
                    ts.setBoardingStop(bStop);
                    ts.setDroppingStop(dStop);
                    ts.setDepartureTime(segReq.getDepartureTime());
                    ts.setArrivalTime(segReq.getArrivalTime());
                    ts.setDepartureDate(segReq.getDepartureDate() != null ? segReq.getDepartureDate() : trip.getTravelDate());
                    ts.setArrivalDate(segReq.getArrivalDate() != null ? segReq.getArrivalDate() : trip.getTravelDate());
                    ts.setFare(segReq.getFare());
                    existingSegments.add(ts);
                    retainedSegments.add(ts);
                }
            }
        }
        
        existingSegments.retainAll(retainedSegments);
        Trip savedTrip = tripRepository.save(trip);

        return convertToDTO(savedTrip);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripDTO> searchTrips(
            TripSearchRequest request) {

        List<String> sourceNames;
        if (request.getFromLocationId() != null) {
            sourceNames = locationService.getAllNamesForLocationId(request.getFromLocationId());
        } else {
            sourceNames = locationService.getAllNamesForLocationNameOrAlias(request.getSource());
        }

        List<String> destinationNames;
        if (request.getToLocationId() != null) {
            destinationNames = locationService.getAllNamesForLocationId(request.getToLocationId());
        } else {
            destinationNames = locationService.getAllNamesForLocationNameOrAlias(request.getDestination());
        }

        List<Trip> trips;

        if (request.getTravelDate() != null) {
            trips = tripRepository
                    .findByIntermediateStopsAndTravelDate(
                            sourceNames,
                            destinationNames,
                            request.getTravelDate()
                    );
        } else {
            trips = tripRepository
                    .findByIntermediateStops(
                            sourceNames,
                            destinationNames
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

                .map(trip -> convertToDTOWithDynamicFare(trip, sourceNames, destinationNames))
                .toList();
    }

    private TripDTO convertToDTOWithDynamicFare(Trip trip, List<String> sourceNames, List<String> destinationNames) {
        TripDTO dto = convertToDTO(trip);
        
        // Helper to check case-insensitive match against list
        java.util.function.Predicate<String> matchesSource = s -> 
                sourceNames.stream().anyMatch(name -> name.equalsIgnoreCase(s));
        java.util.function.Predicate<String> matchesDest = s -> 
                destinationNames.stream().anyMatch(name -> name.equalsIgnoreCase(s));

        // If exact source and destination match the route, baseFare is unchanged
        if (matchesSource.test(trip.getRoute().getSource()) && 
            matchesDest.test(trip.getRoute().getDestination())) {
            return dto;
        }

        // Iterate through the segments to find the sequence from source to destination
        if (dto.getSegments() != null && !dto.getSegments().isEmpty()) {
            int startIndex = -1;
            int endIndex = -1;
            
            for (int i = 0; i < dto.getSegments().size(); i++) {
                com.crimsonlogic.busticketbooking.dto.TripSegmentDTO ts = dto.getSegments().get(i);
                if (startIndex == -1 && matchesSource.test(ts.getBoardingStopName())) {
                    startIndex = i;
                }
                // Update endIndex whenever we find a matching drop point after the start point
                if (startIndex != -1 && matchesDest.test(ts.getDroppingStopName())) {
                    endIndex = i;
                    break;
                }
            }
            
            if (startIndex != -1 && endIndex != -1) {
                // We found a valid sub-route sequence!
                com.crimsonlogic.busticketbooking.dto.TripSegmentDTO startSegment = dto.getSegments().get(startIndex);
                com.crimsonlogic.busticketbooking.dto.TripSegmentDTO endSegment = dto.getSegments().get(endIndex);
                
                dto.setSource(startSegment.getBoardingStopName());
                dto.setDestination(endSegment.getDroppingStopName());
                dto.setDepartureTime(startSegment.getDepartureTime());
                dto.setTravelDate(startSegment.getDepartureDate());
                dto.setArrivalTime(endSegment.getArrivalTime());
                dto.setArrivalDate(endSegment.getArrivalDate());
                
                // If it's a single segment that matches both, use its fare.
                // Otherwise, calculate accumulated fare or use the provided logic.
                // Since baseFare can be dynamic, let's accumulate it from the segments if they have individual fares, 
                // or just rely on a single covering segment if the DB stores it like that.
                // If the DB only stores point-to-point (A->B, B->C), accumulate it:
                java.math.BigDecimal totalFare = java.math.BigDecimal.ZERO;
                for (int i = startIndex; i <= endIndex; i++) {
                    if (dto.getSegments().get(i).getFare() != null) {
                        totalFare = totalFare.add(dto.getSegments().get(i).getFare());
                    }
                }
                
                // If we accumulated a fare > 0, set it. Otherwise fallback to trip base fare.
                if (totalFare.compareTo(java.math.BigDecimal.ZERO) > 0) {
                    dto.setBaseFare(totalFare);
                } else if (trip.getTripSegments() != null && !trip.getTripSegments().isEmpty()) {
                    // Try to find a single covering segment in the DB
                    for (com.crimsonlogic.busticketbooking.entity.TripSegment ts : trip.getTripSegments()) {
                        if (matchesSource.test(ts.getBoardingStop().getStopName()) &&
                            matchesDest.test(ts.getDroppingStop().getStopName())) {
                            dto.setBaseFare(ts.getFare());
                            break;
                        }
                    }
                }
                
                // Finally, truncate the segments list to only include the searched sub-route
                dto.setSegments(dto.getSegments().subList(startIndex, endIndex + 1));
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
    public void cancelTrip(String tripId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Cancellation reason is required");
        }
        
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found"));

        cancelTripInternal(trip, reason);
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

        cancelTripInternal(trip, reason);
    }

    private void cancelTripInternal(Trip trip, String reason) {
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
                        java.util.List.of(source),
                        java.util.List.of(destination),
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

    @Override
    @Transactional(readOnly = true)
    public List<com.crimsonlogic.busticketbooking.dto.PassengerAnalyticsDTO> getTripPassengers(String tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found"));
        authorizeOperatorForBus(trip.getBus());

        java.util.List<com.crimsonlogic.busticketbooking.entity.Booking> bookings = bookingRepository.findByTrip_TripId(tripId);
        java.util.List<com.crimsonlogic.busticketbooking.dto.PassengerAnalyticsDTO> analytics = new java.util.ArrayList<>();

        for (com.crimsonlogic.busticketbooking.entity.Booking booking : bookings) {
            if (booking.getBookingStatus() == BookingStatus.CONFIRMED || booking.getBookingStatus() == BookingStatus.PENDING) {
                for (com.crimsonlogic.busticketbooking.entity.BookingSeat seat : booking.getBookingSeats()) {
                    com.crimsonlogic.busticketbooking.dto.PassengerAnalyticsDTO dto = new com.crimsonlogic.busticketbooking.dto.PassengerAnalyticsDTO();
                    dto.setPassengerName(seat.getPassengerName());
                    dto.setAge(seat.getPassengerAge());
                    dto.setGender(seat.getPassengerGender());
                    dto.setSeatNumber(seat.getTripSeat().getBusSeat().getSeatNumber());
                    dto.setBoardingPoint(booking.getBoardingPoint() != null ? booking.getBoardingPoint().getStopName() : null);
                    dto.setDroppingPoint(booking.getDroppingPoint() != null ? booking.getDroppingPoint().getStopName() : null);
                    dto.setBookingReference(booking.getBookingReference());
                    analytics.add(dto);
                }
            }
        }
        return analytics;
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

            // Derive AC flag: true if amenities contain "AC", otherwise Non-AC (default false)
            boolean hasAc = bus.getAmenities() != null &&
                    bus.getAmenities().stream()
                            .anyMatch(a -> a.equalsIgnoreCase("AC") || a.equalsIgnoreCase("Air Conditioning"));
            dto.setAc(hasAc);

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

        if (trip.getTripSegments() != null && !trip.getTripSegments().isEmpty()) {
            dto.setSegments(trip.getTripSegments().stream().map(ts -> {
                com.crimsonlogic.busticketbooking.dto.TripSegmentDTO tsDTO = new com.crimsonlogic.busticketbooking.dto.TripSegmentDTO();
                tsDTO.setId(ts.getId());
                tsDTO.setTripId(ts.getTrip().getTripId());
                tsDTO.setBoardingStopId(ts.getBoardingStop().getRouteStopId());
                tsDTO.setBoardingStopName(ts.getBoardingStop().getStopName());
                if (ts.getBoardingStop().getFareLocation() != null) {
                    tsDTO.setBoardingZoneName(ts.getBoardingStop().getFareLocation().getName());
                }
                tsDTO.setDepartureTime(ts.getDepartureTime());
                tsDTO.setDepartureDate(ts.getDepartureDate());

                tsDTO.setDroppingStopId(ts.getDroppingStop().getRouteStopId());
                tsDTO.setDroppingStopName(ts.getDroppingStop().getStopName());
                if (ts.getDroppingStop().getFareLocation() != null) {
                    tsDTO.setDroppingZoneName(ts.getDroppingStop().getFareLocation().getName());
                }
                tsDTO.setArrivalTime(ts.getArrivalTime());
                tsDTO.setArrivalDate(ts.getArrivalDate());

                tsDTO.setFare(ts.getFare());
                return tsDTO;
            }).toList());
        } else if (trip.getRoute() != null && trip.getRoute().getRouteStops() != null && !trip.getRoute().getRouteStops().isEmpty()) {
            java.util.List<com.crimsonlogic.busticketbooking.entity.RouteStop> stops = trip.getRoute().getRouteStops().stream()
                    .sorted(java.util.Comparator.comparingInt(com.crimsonlogic.busticketbooking.entity.RouteStop::getStopSequence))
                    .toList();
            
            java.util.List<com.crimsonlogic.busticketbooking.dto.TripSegmentDTO> syntheticSegments = new java.util.ArrayList<>();
            long totalDurationMins = 0;
            if (trip.getDepartureTime() != null && trip.getArrivalTime() != null) {
                totalDurationMins = java.time.temporal.ChronoUnit.MINUTES.between(trip.getDepartureTime(), trip.getArrivalTime());
                if (totalDurationMins < 0) totalDurationMins += 24 * 60;
            }
            
            // Use the distance of the last stop as the actual total distance to ensure fractions never exceed 1.0
            java.math.BigDecimal totalDistance = stops.get(stops.size() - 1).getDistanceFromSourceKm();
            if (totalDistance == null || totalDistance.compareTo(java.math.BigDecimal.ZERO) == 0) {
                totalDistance = trip.getRoute().getDistance(); // Fallback
            }
            
            for (int i = 0; i < stops.size() - 1; i++) {
                com.crimsonlogic.busticketbooking.entity.RouteStop bStop = stops.get(i);
                com.crimsonlogic.busticketbooking.entity.RouteStop dStop = stops.get(i + 1);
                
                com.crimsonlogic.busticketbooking.dto.TripSegmentDTO tsDTO = new com.crimsonlogic.busticketbooking.dto.TripSegmentDTO();
                tsDTO.setId("synth-" + i);
                tsDTO.setTripId(trip.getTripId());
                
                tsDTO.setBoardingStopId(bStop.getRouteStopId());
                tsDTO.setBoardingStopName(bStop.getStopName());
                if (bStop.getFareLocation() != null) tsDTO.setBoardingZoneName(bStop.getFareLocation().getName());
                
                if (i == 0) {
                    tsDTO.setDepartureTime(trip.getDepartureTime());
                } else if (trip.getDepartureTime() != null && totalDistance != null && totalDistance.compareTo(java.math.BigDecimal.ZERO) > 0) {
                    double fraction = bStop.getDistanceFromSourceKm().doubleValue() / totalDistance.doubleValue();
                    // Cap fraction at 1.0 to avoid overshooting
                    if (fraction > 1.0) fraction = 1.0;
                    long minsToAdd = (long) (totalDurationMins * fraction);
                    tsDTO.setDepartureTime(trip.getDepartureTime().plusMinutes(minsToAdd));
                } else {
                    tsDTO.setDepartureTime(trip.getDepartureTime());
                }
                tsDTO.setDepartureDate(trip.getTravelDate());
                
                tsDTO.setDroppingStopId(dStop.getRouteStopId());
                tsDTO.setDroppingStopName(dStop.getStopName());
                if (dStop.getFareLocation() != null) tsDTO.setDroppingZoneName(dStop.getFareLocation().getName());
                
                if (i == stops.size() - 2) {
                    tsDTO.setArrivalTime(trip.getArrivalTime());
                } else if (trip.getDepartureTime() != null && totalDistance != null && totalDistance.compareTo(java.math.BigDecimal.ZERO) > 0) {
                    double fraction = dStop.getDistanceFromSourceKm().doubleValue() / totalDistance.doubleValue();
                    if (fraction > 1.0) fraction = 1.0;
                    long minsToAdd = (long) (totalDurationMins * fraction);
                    tsDTO.setArrivalTime(trip.getDepartureTime().plusMinutes(minsToAdd));
                } else {
                    tsDTO.setArrivalTime(trip.getArrivalTime());
                }
                tsDTO.setArrivalDate(trip.getArrivalDate() != null ? trip.getArrivalDate() : trip.getTravelDate());
                
                tsDTO.setFare(trip.getBaseFare());
                syntheticSegments.add(tsDTO);
            }
            dto.setSegments(syntheticSegments);
        } else {
            dto.setSegments(new java.util.ArrayList<>());
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

    private void validateSegmentsTiming(java.util.List<com.crimsonlogic.busticketbooking.dto.TripSegmentCreateRequest> segments) {
        if (segments == null || segments.isEmpty()) return;

        java.util.Map<String, String> boardingPointToTime = new java.util.HashMap<>();
        java.util.Map<String, String> timeToBoardingPoint = new java.util.HashMap<>();
        
        java.util.Map<String, String> droppingPointToTime = new java.util.HashMap<>();
        java.util.Map<String, String> timeToDroppingPoint = new java.util.HashMap<>();

        java.util.Set<String> segmentPairs = new java.util.HashSet<>();

        for (com.crimsonlogic.busticketbooking.dto.TripSegmentCreateRequest seg : segments) {
            String pairKey = seg.getBoardingStopId() + "-" + seg.getDroppingStopId();
            if (!segmentPairs.add(pairKey)) {
                throw new IllegalArgumentException("This boarding point and dropping point combination already exists.");
            }

            String depTimeKey = (seg.getDepartureDate() != null ? seg.getDepartureDate().toString() : "") + "-" + (seg.getDepartureTime() != null ? seg.getDepartureTime().toString() : "");
            String arrTimeKey = (seg.getArrivalDate() != null ? seg.getArrivalDate().toString() : "") + "-" + (seg.getArrivalTime() != null ? seg.getArrivalTime().toString() : "");

            if (boardingPointToTime.containsKey(seg.getBoardingStopId())) {
                if (!boardingPointToTime.get(seg.getBoardingStopId()).equals(depTimeKey)) {
                    throw new IllegalArgumentException("The same boarding point cannot have different departure times or days.");
                }
            } else {
                boardingPointToTime.put(seg.getBoardingStopId(), depTimeKey);
            }

            if (timeToBoardingPoint.containsKey(depTimeKey)) {
                if (!timeToBoardingPoint.get(depTimeKey).equals(seg.getBoardingStopId())) {
                    throw new IllegalArgumentException("Different boarding points cannot have the same departure time and day.");
                }
            } else {
                timeToBoardingPoint.put(depTimeKey, seg.getBoardingStopId());
            }

            if (droppingPointToTime.containsKey(seg.getDroppingStopId())) {
                if (!droppingPointToTime.get(seg.getDroppingStopId()).equals(arrTimeKey)) {
                    throw new IllegalArgumentException("The same dropping point cannot have different arrival times or days.");
                }
            } else {
                droppingPointToTime.put(seg.getDroppingStopId(), arrTimeKey);
            }

            if (timeToDroppingPoint.containsKey(arrTimeKey)) {
                if (!timeToDroppingPoint.get(arrTimeKey).equals(seg.getDroppingStopId())) {
                    throw new IllegalArgumentException("Different dropping points cannot have the same arrival time and day.");
                }
            } else {
                timeToDroppingPoint.put(arrTimeKey, seg.getDroppingStopId());
            }
        }
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