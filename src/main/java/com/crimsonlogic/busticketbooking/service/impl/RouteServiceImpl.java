package com.crimsonlogic.busticketbooking.service.impl;

import com.crimsonlogic.busticketbooking.dto.RouteCreateRequest;
import com.crimsonlogic.busticketbooking.dto.RouteDTO;
import com.crimsonlogic.busticketbooking.entity.Route;
import com.crimsonlogic.busticketbooking.repository.BookingRepository;
import com.crimsonlogic.busticketbooking.repository.RouteRepository;
import com.crimsonlogic.busticketbooking.service.RouteService;
import com.crimsonlogic.busticketbooking.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final BookingRepository bookingRepository;

    /** Booking statuses that constitute an "active" booking. */
    private static final List<BookingStatus> ACTIVE_STATUSES =
            List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING);

    @Override
    public RouteDTO createRoute(
            RouteCreateRequest request) {

        /*
         * A route from A → B should be unique.
         */
        if (routeRepository
                .existsBySourceIgnoreCaseAndDestinationIgnoreCase(
                        request.getSource(),
                        request.getDestination()
                )) {

            throw new IllegalArgumentException(
                    "Route from '"
                            + request.getSource()
                            + "' to '"
                            + request.getDestination()
                            + "' already exists"
            );
        }

        /*
         * Source and destination should not be the same.
         */
        if (request.getSource()
                .equalsIgnoreCase(request.getDestination())) {

            throw new IllegalArgumentException(
                    "Source and destination cannot be the same"
            );
        }

        Route route = new Route();

        route.setSource(
                request.getSource()
        );

        route.setDestination(
                request.getDestination()
        );

        route.setDistance(
                request.getDistance()
        );

        /*
         * Newly created routes are active.
         */
        route.setIsActive(true);

        Route savedRoute =
                routeRepository.save(route);

        return convertToDTO(savedRoute);
    }

    @Override
    @Transactional(readOnly = true)
    public RouteDTO getRoute(
            String source,
            String destination) {

        Route route = routeRepository
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

        return convertToDTO(route);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteDTO> getAllRoutes() {

        return routeRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteDTO> getRoutesBySource(
            String source) {

        return routeRepository
                .findBySourceIgnoreCase(source)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RouteDTO> getRoutesByDestination(
            String destination) {

        return routeRepository
                .findByDestinationIgnoreCase(destination)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public RouteDTO updateRoute(
            String source,
            String destination,
            RouteCreateRequest request) {

        /*
         * Find the existing route using its
         * business-facing source and destination.
         */
        Route existingRoute = routeRepository
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

        /*
         * Source and destination cannot be the same.
         */
        if (request.getSource()
                .equalsIgnoreCase(request.getDestination())) {

            throw new IllegalArgumentException(
                    "Source and destination cannot be the same"
            );
        }

        /*
         * If source/destination are being changed,
         * make sure another route doesn't already
         * use the new combination.
         */
        boolean routeChanged =
                !existingRoute.getSource()
                        .equalsIgnoreCase(request.getSource())
                        ||
                        !existingRoute.getDestination()
                                .equalsIgnoreCase(request.getDestination());

        if (routeChanged &&
                routeRepository
                        .existsBySourceIgnoreCaseAndDestinationIgnoreCase(
                                request.getSource(),
                                request.getDestination()
                        )) {

            throw new IllegalArgumentException(
                    "Route from '"
                            + request.getSource()
                            + "' to '"
                            + request.getDestination()
                            + "' already exists"
            );
        }

        /*
         * ── Active-booking protection (Phase 23) ───────────────────
         * Block any structural modification (source/destination) if the 
         * route is scheduled for upcoming trips that have active bookings.
         */
        if (routeChanged) {
            long activeFutureBookings = bookingRepository
                    .countActiveFutureBookingsByRouteId(
                            existingRoute.getRouteId(),
                            ACTIVE_STATUSES
                    );

            if (activeFutureBookings > 0) {
                throw new IllegalArgumentException(
                        "Cannot modify route from '" + existingRoute.getSource()
                                + "' to '" + existingRoute.getDestination()
                                + "': it has " + activeFutureBookings
                                + " active booking(s) on upcoming trips. "
                                + "Structural changes are blocked until those trips conclude."
                );
            }
        }
        // ──────────────────────────────────────────────────────────

        /*
         * Update editable route information.
         */
        existingRoute.setSource(
                request.getSource()
        );

        existingRoute.setDestination(
                request.getDestination()
        );

        existingRoute.setDistance(
                request.getDistance()
        );

        Route updatedRoute =
                routeRepository.save(existingRoute);

        return convertToDTO(updatedRoute);
    }

    @Override
    public void deactivateRoute(
            String source,
            String destination) {

        Route route = routeRepository
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

        if (!Boolean.TRUE.equals(
                route.getIsActive())) {

            throw new IllegalArgumentException(
                    "Route is already inactive"
            );
        }

        /*
         * ── Active-booking protection (Phase 23) ───────────────────
         * A route cannot be deactivated if passengers have already booked seats
         * on any upcoming trip operated on this route.
         */
        long activeFutureBookings = bookingRepository
                .countActiveFutureBookingsByRouteId(
                        route.getRouteId(),
                        ACTIVE_STATUSES
                );

        if (activeFutureBookings > 0) {
            throw new IllegalArgumentException(
                    "Cannot deactivate route from '" + route.getSource()
                            + "' to '" + route.getDestination()
                            + "': it has " + activeFutureBookings
                            + " active booking(s) on upcoming trips. "
                            + "The route must remain active until those trips conclude."
            );
        }
        // ──────────────────────────────────────────────────────────

        /*
         * Soft deactivation.
         *
         * Historical trips can still reference this route,
         * so we don't physically delete it.
         */
        route.setIsActive(false);

        routeRepository.save(route);
    }

    /*
     * Entity → DTO
     */
    private RouteDTO convertToDTO(Route route) {

        RouteDTO dto = new RouteDTO();

        dto.setRouteId(
                route.getRouteId()
        );

        dto.setSource(
                route.getSource()
        );

        dto.setDestination(
                route.getDestination()
        );

        dto.setDistance(
                route.getDistance()
        );

        dto.setIsActive(
                route.getIsActive()
        );

        return dto;
    }
}