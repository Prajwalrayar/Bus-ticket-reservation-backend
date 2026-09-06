package com.crimsonlogic.busticketbooking.service.impl;

import com.crimsonlogic.busticketbooking.dto.BusCreateRequest;
import com.crimsonlogic.busticketbooking.dto.BusDTO;
import com.crimsonlogic.busticketbooking.dto.BusSeatCreateRequest;
import com.crimsonlogic.busticketbooking.dto.BusSeatDTO;
import com.crimsonlogic.busticketbooking.entity.Bus;
import com.crimsonlogic.busticketbooking.entity.BusSeat;
import com.crimsonlogic.busticketbooking.entity.Operator;
import com.crimsonlogic.busticketbooking.enums.BookingStatus;
import com.crimsonlogic.busticketbooking.repository.BookingRepository;
import com.crimsonlogic.busticketbooking.repository.BusRepository;
import com.crimsonlogic.busticketbooking.repository.BusSeatRepository;
import com.crimsonlogic.busticketbooking.repository.OperatorRepository;
import com.crimsonlogic.busticketbooking.repository.UserRepository;
import com.crimsonlogic.busticketbooking.service.BusService;
import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BusServiceImpl implements BusService {

    private final BusRepository busRepository;
    private final BusSeatRepository busSeatRepository;
    private final OperatorRepository operatorRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    /** Booking statuses that constitute an "active" booking for Phase 6 guards. */
    private static final List<BookingStatus> ACTIVE_STATUSES =
            List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING);

    private void authorizeOperatorAccess(String targetCompanyName) {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        com.crimsonlogic.busticketbooking.entity.User currentUser = userRepository.findByUserEmailIgnoreCase(currentUserEmail)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));

        boolean isAdmin = currentUser.getUserRoles().stream()
                .anyMatch(role -> role.getRoleName().equals("ADMIN"));

        if (!isAdmin) {
            if (currentUser.getOperator() == null || !currentUser.getOperator().getCompanyName().equalsIgnoreCase(targetCompanyName)) {
                throw new AccessDeniedException("You are not authorized to manage data for this operating company.");
            }
        }
    }


    // =========================================================
    // BUS OPERATIONS
    // =========================================================

    @Override
    public BusDTO createBus(BusCreateRequest request) {
        authorizeOperatorAccess(request.getOperatorCompanyName());

        Operator operator = operatorRepository
                .findByCompanyNameIgnoreCase(
                        request.getOperatorCompanyName()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Operator with company name '"
                                        + request.getOperatorCompanyName()
                                        + "' not found"
                        )
                );

        if (!Boolean.TRUE.equals(operator.getIsApproved())) {
            throw new IllegalArgumentException(
                    "Operator is not approved"
            );
        }

        if (!Boolean.TRUE.equals(operator.getIsActive())) {
            throw new IllegalArgumentException(
                    "Operator is inactive"
            );
        }

        if (busRepository.existsByRegistrationNumberIgnoreCase(
                request.getRegistrationNumber())) {

            throw new IllegalArgumentException(
                    "Bus with registration number '"
                            + request.getRegistrationNumber()
                            + "' already exists"
            );
        }

        Bus bus = new Bus();

        bus.setBusId(generateId());

        bus.setRegistrationNumber(
                request.getRegistrationNumber().trim()
        );

        bus.setBusType(
                request.getBusType()
        );

        if (request.getAmenities() != null) {
            bus.setAmenities(
                    request.getAmenities()
            );
        }

        bus.setOperator(operator);
        bus.setIsActive(true);

        return convertToBusDTO(
                busRepository.save(bus)
        );
    }


    @Override
    @Transactional(readOnly = true)
    public BusDTO getBusByRegistrationNumber(
            String registrationNumber) {

        Bus bus = findBus(registrationNumber);

        return convertToBusDTO(bus);
    }


    @Override
    @Transactional(readOnly = true)
    public List<BusDTO> getAllBuses() {

        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        com.crimsonlogic.busticketbooking.entity.User currentUser = userRepository.findByUserEmailIgnoreCase(currentUserEmail)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));

        boolean isAdmin = currentUser.getUserRoles().stream()
                .anyMatch(role -> role.getRoleName().equals("ADMIN"));

        List<Bus> buses;
        if (isAdmin || currentUser.getOperator() == null) {
            buses = busRepository.findAll();
        } else {
            buses = busRepository.findByOperator_CompanyNameIgnoreCase(currentUser.getOperator().getCompanyName());
        }

        return buses.stream()
                .map(this::convertToBusDTO)
                .toList();
    }


    @Override
    @Transactional(readOnly = true)
    public List<BusDTO> getBusesByOperator(
            String companyName) {
            
        authorizeOperatorAccess(companyName);

        operatorRepository
                .findByCompanyNameIgnoreCase(companyName)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Operator with company name '"
                                        + companyName
                                        + "' not found"
                        )
                );

        return busRepository
                .findByOperator_CompanyNameIgnoreCase(
                        companyName
                )
                .stream()
                .map(this::convertToBusDTO)
                .toList();
    }


    @Override
    public BusDTO updateBus(
            String registrationNumber,
            BusCreateRequest request) {

        Bus existingBus =
                findBus(registrationNumber);

        authorizeOperatorAccess(existingBus.getOperator().getCompanyName());
        if (request.getOperatorCompanyName() != null) {
            authorizeOperatorAccess(request.getOperatorCompanyName());
        }

        // ── Active-booking protection (Phase 6) ───────────────────
        // Block destructive structural changes (registration number or bus type)
        // while the bus has CONFIRMED/PENDING bookings on upcoming trips.
        // Amenity-only updates are always permitted.
        if (isDestructiveChange(existingBus, request)) {
            long activeFutureBookings = bookingRepository
                    .countActiveFutureBookingsByBusId(
                            existingBus.getBusId(),
                            ACTIVE_STATUSES
                    );

            if (activeFutureBookings > 0) {
                throw new IllegalArgumentException(
                        "Cannot modify bus '" + existingBus.getRegistrationNumber()
                                + "': it has " + activeFutureBookings
                                + " active booking(s) on upcoming trips. "
                                + "Structural changes (type/registration) are blocked "
                                + "until those trips conclude."
                );
            }
        }
        // ──────────────────────────────────────────────────────────

        if (!existingBus.getRegistrationNumber()
                .equalsIgnoreCase(
                        request.getRegistrationNumber()
                )
                && busRepository
                .existsByRegistrationNumberIgnoreCase(
                        request.getRegistrationNumber()
                )) {

            throw new IllegalArgumentException(
                    "Another bus with registration number '"
                            + request.getRegistrationNumber()
                            + "' already exists"
            );
        }

        existingBus.setRegistrationNumber(
                request.getRegistrationNumber().trim()
        );

        existingBus.setBusType(
                request.getBusType()
        );

        if (request.getAmenities() != null) {
            existingBus.setAmenities(
                    request.getAmenities()
            );
        }

        /*
         * Operator is deliberately not changed.
         * Operator transfer should be a separate business operation.
         */

        return convertToBusDTO(
                busRepository.save(existingBus)
        );
    }


    @Override
    public void deactivateBus(
            String registrationNumber) {

        Bus bus = findBus(registrationNumber);
        
        authorizeOperatorAccess(bus.getOperator().getCompanyName());

        if (!Boolean.TRUE.equals(bus.getIsActive())) {
            throw new IllegalArgumentException(
                    "Bus is already inactive"
            );
        }

        // ── Active-booking protection (Phase 6) ───────────────────
        // A bus cannot be deactivated if passengers have already booked seats
        // on any upcoming trip operated by this bus.
        long activeFutureBookings = bookingRepository
                .countActiveFutureBookingsByBusId(
                        bus.getBusId(),
                        ACTIVE_STATUSES
                );

        if (activeFutureBookings > 0) {
            throw new IllegalArgumentException(
                    "Cannot deactivate bus '" + bus.getRegistrationNumber()
                            + "': it has " + activeFutureBookings
                            + " active booking(s) on upcoming trips. "
                            + "The bus must remain active until those trips conclude."
            );
        }
        // ──────────────────────────────────────────────────────────

        /*
         * Soft deactivation is important because historical
         * trips and bookings can reference this bus.
         */
        bus.setIsActive(false);

        busRepository.save(bus);
    }


    // =========================================================
    // BUS SEAT OPERATIONS
    // =========================================================

    @Override
    public BusSeatDTO createBusSeat(
            String busRegistrationNumber,
            BusSeatCreateRequest request) {

        Bus bus = findBus(busRegistrationNumber);

        // Verify the operator owns this bus before adding seats
        authorizeOperatorAccess(bus.getOperator().getCompanyName());

        if (!Boolean.TRUE.equals(bus.getIsActive())) {
            throw new IllegalArgumentException(
                    "Cannot add a seat to an inactive bus"
            );
        }

        if (busSeatRepository
                .existsByBus_RegistrationNumberIgnoreCaseAndSeatNumberIgnoreCase(
                        busRegistrationNumber,
                        request.getSeatNumber()
                )) {

            throw new IllegalArgumentException(
                    "Seat number '"
                            + request.getSeatNumber()
                            + "' already exists on this bus"
            );
        }

        BusSeat busSeat = new BusSeat();

        busSeat.setBusSeatId(generateId());

        busSeat.setSeatNumber(
                request.getSeatNumber().trim()
        );

        busSeat.setSeatPosition(
                request.getSeatPosition()
        );

        busSeat.setIsActive(true);
        busSeat.setBus(bus);

        return convertToBusSeatDTO(
                busSeatRepository.save(busSeat)
        );
    }


    @Override
    @Transactional(readOnly = true)
    public BusSeatDTO getBusSeat(
            String busRegistrationNumber,
            String seatNumber) {

        BusSeat busSeat =
                busSeatRepository
                        .findByBus_RegistrationNumberIgnoreCaseAndSeatNumberIgnoreCase(
                                busRegistrationNumber,
                                seatNumber
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Seat '"
                                                + seatNumber
                                                + "' not found on bus '"
                                                + busRegistrationNumber
                                                + "'"
                                )
                        );

        return convertToBusSeatDTO(busSeat);
    }


    @Override
    @Transactional(readOnly = true)
    public List<BusSeatDTO> getSeatsByBus(
            String busRegistrationNumber) {

        findBus(busRegistrationNumber);

        return busSeatRepository
                .findByBus_RegistrationNumberIgnoreCase(
                        busRegistrationNumber
                )
                .stream()
                .map(this::convertToBusSeatDTO)
                .toList();
    }


    @Override
    public BusSeatDTO updateBusSeat(
            String busRegistrationNumber,
            String seatNumber,
            BusSeatCreateRequest request) {

        BusSeat existingSeat =
                busSeatRepository
                        .findByBus_RegistrationNumberIgnoreCaseAndSeatNumberIgnoreCase(
                                busRegistrationNumber,
                                seatNumber
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Seat '"
                                                + seatNumber
                                                + "' not found on bus '"
                                                + busRegistrationNumber
                                                + "'"
                                )
                        );

        // Verify the operator owns this bus before updating seats
        authorizeOperatorAccess(existingSeat.getBus().getOperator().getCompanyName());

        if (!existingSeat.getSeatNumber()
                .equalsIgnoreCase(
                        request.getSeatNumber()
                )
                && busSeatRepository
                .existsByBus_RegistrationNumberIgnoreCaseAndSeatNumberIgnoreCase(
                        busRegistrationNumber,
                        request.getSeatNumber()
                )) {

            throw new IllegalArgumentException(
                    "Seat number '"
                            + request.getSeatNumber()
                            + "' already exists on this bus"
            );
        }

        existingSeat.setSeatNumber(
                request.getSeatNumber().trim()
        );

        existingSeat.setSeatPosition(
                request.getSeatPosition()
        );

        return convertToBusSeatDTO(
                busSeatRepository.save(existingSeat)
        );
    }


    @Override
    public void deactivateBusSeat(
            String busRegistrationNumber,
            String seatNumber) {

        BusSeat busSeat =
                busSeatRepository
                        .findByBus_RegistrationNumberIgnoreCaseAndSeatNumberIgnoreCase(
                                busRegistrationNumber,
                                seatNumber
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Seat '"
                                                + seatNumber
                                                + "' not found on bus '"
                                                + busRegistrationNumber
                                                + "'"
                                )
                        );

        // Verify the operator owns this bus before deactivating seats
        authorizeOperatorAccess(busSeat.getBus().getOperator().getCompanyName());

        if (!Boolean.TRUE.equals(
                busSeat.getIsActive())) {

            throw new IllegalArgumentException(
                    "Seat is already inactive"
            );
        }

        busSeat.setIsActive(false);

        busSeatRepository.save(busSeat);
    }


    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    private Bus findBus(String registrationNumber) {

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


    private BusDTO convertToBusDTO(Bus bus) {

        BusDTO dto = new BusDTO();

        dto.setBusId(
                bus.getBusId()
        );

        dto.setRegistrationNumber(
                bus.getRegistrationNumber()
        );

        dto.setBusType(
                bus.getBusType()
        );

        dto.setAmenities(
                bus.getAmenities()
        );

        dto.setIsActive(
                bus.getIsActive()
        );

        if (bus.getOperator() != null) {
            dto.setOperatorCompanyName(
                    bus.getOperator()
                            .getCompanyName()
            );
        }

        return dto;
    }


    private BusSeatDTO convertToBusSeatDTO(
            BusSeat busSeat) {

        BusSeatDTO dto = new BusSeatDTO();

        dto.setBusSeatId(
                busSeat.getBusSeatId()
        );

        dto.setSeatNumber(
                busSeat.getSeatNumber()
        );

        dto.setSeatPosition(
                busSeat.getSeatPosition()
        );

        dto.setIsActive(
                busSeat.getIsActive()
        );

        if (busSeat.getBus() != null) {
            dto.setBusRegistrationNumber(
                    busSeat.getBus()
                            .getRegistrationNumber()
            );
        }

        return dto;
    }


    private String generateId() {
        return EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_BUS);
    }

    /**
     * A change is considered "destructive" if it alters the bus's structural
     * identity — specifically its registration number or bus type.
     *
     * Passengers choose a bus partly based on its type (Sleeper vs Seater).
     * Changing these after bookings exist would silently break the contract
     * with booked passengers.
     *
     * Amenity changes (e.g. adding WiFi) are non-destructive and always allowed.
     */
    private boolean isDestructiveChange(Bus existing, BusCreateRequest request) {
        boolean regChanged = !existing.getRegistrationNumber()
                .equalsIgnoreCase(request.getRegistrationNumber());
        boolean typeChanged = existing.getBusType() != null
                && !existing.getBusType().equals(request.getBusType());
        return regChanged || typeChanged;
    }
}