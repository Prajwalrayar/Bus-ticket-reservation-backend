package com.crimsonlogic.busticketbooking.service.impl;

import com.crimsonlogic.busticketbooking.dto.*;
import com.crimsonlogic.busticketbooking.entity.Bus;
import com.crimsonlogic.busticketbooking.entity.BusSeat;
import com.crimsonlogic.busticketbooking.entity.Operator;
import com.crimsonlogic.busticketbooking.enums.BookingStatus;
import com.crimsonlogic.busticketbooking.enums.BusActivationStatus;
import com.crimsonlogic.busticketbooking.repository.BookingRepository;
import com.crimsonlogic.busticketbooking.repository.BusRepository;
import com.crimsonlogic.busticketbooking.repository.BusSeatRepository;
import com.crimsonlogic.busticketbooking.repository.OperatorRepository;
import com.crimsonlogic.busticketbooking.repository.UserRepository;
import com.crimsonlogic.busticketbooking.service.BusService;
import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BusServiceImpl implements BusService {

    private final BusRepository busRepository;
    private final BusSeatRepository busSeatRepository;
    private final OperatorRepository operatorRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpaySecret;

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

    private void requireAdmin() {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        com.crimsonlogic.busticketbooking.entity.User currentUser = userRepository.findByUserEmailIgnoreCase(currentUserEmail)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));
        boolean isAdmin = currentUser.getUserRoles().stream()
                .anyMatch(role -> role.getRoleName().equals("ADMIN"));
        if (!isAdmin) {
            throw new AccessDeniedException("Only administrators can perform this action.");
        }
    }

    // =========================================================
    // BUS OPERATIONS
    // =========================================================

    @Override
    public BusDTO createBus(BusCreateRequest request) {
        authorizeOperatorAccess(request.getOperatorCompanyName());

        Operator operator = operatorRepository
                .findByCompanyNameIgnoreCase(request.getOperatorCompanyName())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Operator with company name '" + request.getOperatorCompanyName() + "' not found"));

        if (!Boolean.TRUE.equals(operator.getIsApproved())) {
            throw new IllegalArgumentException("Operator is not approved");
        }

        if (!Boolean.TRUE.equals(operator.getIsActive())) {
            throw new IllegalArgumentException("Operator is inactive");
        }

        if (busRepository.existsByRegistrationNumberIgnoreCase(request.getRegistrationNumber())) {
            throw new IllegalArgumentException(
                    "Bus with registration number '" + request.getRegistrationNumber() + "' already exists");
        }

        Bus bus = new Bus();
        bus.setBusId(generateId());
        bus.setRegistrationNumber(request.getRegistrationNumber().trim());
        bus.setBusType(request.getBusType());

        if (request.getAmenities() != null) {
            bus.setAmenities(request.getAmenities());
        }

        bus.setOperator(operator);
        bus.setIsActive(true);
        bus.setPetsAllowed(request.getPetsAllowed() != null ? request.getPetsAllowed() : false);
        bus.setBaggagePolicy(request.getBaggagePolicy());
        bus.setActivationRequestStatus(BusActivationStatus.NONE);

        return convertToBusDTO(busRepository.save(bus));
    }


    @Override
    @Transactional(readOnly = true)
    public BusDTO getBusByRegistrationNumber(String registrationNumber) {
        return convertToBusDTO(findBus(registrationNumber));
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

        return buses.stream().map(this::convertToBusDTO).toList();
    }


    @Override
    @Transactional(readOnly = true)
    public List<BusDTO> getBusesByOperator(String companyName) {
        authorizeOperatorAccess(companyName);

        operatorRepository.findByCompanyNameIgnoreCase(companyName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Operator with company name '" + companyName + "' not found"));

        return busRepository.findByOperator_CompanyNameIgnoreCase(companyName)
                .stream().map(this::convertToBusDTO).toList();
    }


    @Override
    public BusDTO updateBus(String registrationNumber, BusCreateRequest request) {
        Bus existingBus = findBus(registrationNumber);
        authorizeOperatorAccess(existingBus.getOperator().getCompanyName());
        if (request.getOperatorCompanyName() != null) {
            authorizeOperatorAccess(request.getOperatorCompanyName());
        }

        if (isDestructiveChange(existingBus, request)) {
            long activeFutureBookings = bookingRepository
                    .countActiveFutureBookingsByBusId(existingBus.getBusId(), ACTIVE_STATUSES);

            if (activeFutureBookings > 0) {
                throw new IllegalArgumentException(
                        "Cannot modify bus '" + existingBus.getRegistrationNumber()
                                + "': it has " + activeFutureBookings
                                + " active booking(s) on upcoming trips. "
                                + "Structural changes (type/registration) are blocked "
                                + "until those trips conclude.");
            }
        }

        if (!existingBus.getRegistrationNumber().equalsIgnoreCase(request.getRegistrationNumber())
                && busRepository.existsByRegistrationNumberIgnoreCase(request.getRegistrationNumber())) {
            throw new IllegalArgumentException(
                    "Another bus with registration number '" + request.getRegistrationNumber() + "' already exists");
        }

        existingBus.setRegistrationNumber(request.getRegistrationNumber().trim());
        existingBus.setBusType(request.getBusType());

        if (request.getAmenities() != null) {
            existingBus.setAmenities(request.getAmenities());
        }
        if (request.getPetsAllowed() != null) {
            existingBus.setPetsAllowed(request.getPetsAllowed());
        }
        if (request.getBaggagePolicy() != null) {
            existingBus.setBaggagePolicy(request.getBaggagePolicy());
        }

        return convertToBusDTO(busRepository.save(existingBus));
    }


    // =========================================================
    // ACTIVATION REQUEST WORKFLOW
    // =========================================================

    @Override
    public void requestActivation(String registrationNumber, String reason) {
        Bus bus = findBus(registrationNumber);
        authorizeOperatorAccess(bus.getOperator().getCompanyName());

        if (Boolean.TRUE.equals(bus.getIsActive())) {
            throw new IllegalArgumentException("Bus is already active.");
        }

        BusActivationStatus status = bus.getActivationRequestStatus();
        if (status == BusActivationStatus.PENDING) {
            throw new IllegalArgumentException("An activation request is already pending for this bus.");
        }
        if (status == BusActivationStatus.APPROVED) {
            throw new IllegalArgumentException("Request already approved. Please complete payment to activate.");
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason for reactivation is required.");
        }

        bus.setActivationRequestStatus(BusActivationStatus.PENDING);
        bus.setActivationRequestNote(reason.trim());
        bus.setActivationRequestedAt(LocalDateTime.now());
        bus.setCompensationAmount(null);
        bus.setAdminRejectionNote(null);
        bus.setActivationApprovedAt(null);
        busRepository.save(bus);
    }


    @Override
    public void approveActivationRequest(String registrationNumber, BigDecimal compensationAmount, String adminNote) {
        requireAdmin();
        Bus bus = findBus(registrationNumber);

        if (bus.getActivationRequestStatus() != BusActivationStatus.PENDING) {
            throw new IllegalArgumentException("No pending activation request for this bus.");
        }

        if (compensationAmount == null || compensationAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Compensation amount must be >= 0.");
        }

        bus.setActivationRequestStatus(BusActivationStatus.APPROVED);
        bus.setCompensationAmount(compensationAmount);
        bus.setActivationApprovedAt(LocalDateTime.now());
        bus.setAdminRejectionNote(adminNote);
        busRepository.save(bus);
    }


    @Override
    public void rejectActivationRequest(String registrationNumber, String adminNote) {
        requireAdmin();
        Bus bus = findBus(registrationNumber);

        if (bus.getActivationRequestStatus() != BusActivationStatus.PENDING) {
            throw new IllegalArgumentException("No pending activation request for this bus.");
        }

        bus.setActivationRequestStatus(BusActivationStatus.REJECTED);
        bus.setAdminRejectionNote(adminNote);
        busRepository.save(bus);
    }


    @Override
    @Transactional(readOnly = true)
    public List<BusDTO> getPendingActivationRequests() {
        requireAdmin();
        return busRepository.findByActivationRequestStatus(BusActivationStatus.PENDING)
                .stream().map(this::convertToBusDTO).toList();
    }


    @Override
    public RazorpayOrderResponse createCompensationRazorpayOrder(String registrationNumber) {
        Bus bus = findBus(registrationNumber);
        authorizeOperatorAccess(bus.getOperator().getCompanyName());

        if (bus.getActivationRequestStatus() != BusActivationStatus.APPROVED) {
            throw new IllegalArgumentException("Bus activation has not been approved by admin yet.");
        }

        BigDecimal amount = bus.getCompensationAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Invalid compensation amount.");
        }

        RazorpayOrderResponse response = new RazorpayOrderResponse();

        // If compensation is zero, skip Razorpay and activate directly
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            activateBusInternal(bus);
            response.setOrderId(null);
            response.setAmount(BigDecimal.ZERO);
            response.setKeyId(razorpayKeyId);
            response.setCurrency("INR");
            return response;
        }

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount.multiply(new BigDecimal("100")).intValue());
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "BUS_ACTIVATION_" + bus.getBusId());

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);

            response.setOrderId(razorpayOrder.get("id"));
            response.setKeyId(razorpayKeyId);
            response.setAmount(amount);
            response.setCurrency("INR");
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay Order for bus activation", e);
            throw new RuntimeException("Failed to create Razorpay Order: " + e.getMessage());
        }

        return response;
    }


    @Override
    public void verifyCompensationAndActivate(String registrationNumber, RazorpayVerificationRequest request) {
        Bus bus = findBus(registrationNumber);
        authorizeOperatorAccess(bus.getOperator().getCompanyName());

        if (bus.getActivationRequestStatus() != BusActivationStatus.APPROVED) {
            throw new IllegalArgumentException("Bus activation has not been approved by admin yet.");
        }

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            boolean isValid = Utils.verifyPaymentSignature(options, razorpaySecret);

            if (isValid) {
                activateBusInternal(bus);
                log.info("Bus {} successfully activated after compensation payment.", registrationNumber);
            } else {
                throw new IllegalArgumentException("Payment verification failed. Invalid signature.");
            }
        } catch (RazorpayException e) {
            log.error("Razorpay verification error for bus activation", e);
            throw new RuntimeException("Payment verification error: " + e.getMessage());
        }
    }


    private void activateBusInternal(Bus bus) {
        bus.setIsActive(true);
        bus.setActivationRequestStatus(BusActivationStatus.NONE);
        bus.setActivationRequestNote(null);
        bus.setCompensationAmount(null);
        bus.setAdminRejectionNote(null);
        bus.setActivationRequestedAt(null);
        bus.setActivationApprovedAt(null);
        busRepository.save(bus);
    }


    // =========================================================
    // AUTO-DEACTIVATION (called by scheduler)
    // =========================================================

    @Override
    public void autoDeactivateInactiveBuses() {
        LocalDate cutoff = LocalDate.now().minusDays(10);
        List<Bus> inactive = busRepository.findActiveBusesInactiveSince(cutoff);
        for (Bus bus : inactive) {
            bus.setIsActive(false);
            // Reset activation status so operator can request reactivation
            bus.setActivationRequestStatus(BusActivationStatus.NONE);
            log.info("Auto-deactivated bus {} due to 10-day inactivity.", bus.getRegistrationNumber());
        }
        busRepository.saveAll(inactive);
    }


    // =========================================================
    // BUS SEAT OPERATIONS
    // =========================================================

    @Override
    public BusSeatDTO createBusSeat(String busRegistrationNumber, BusSeatCreateRequest request) {
        Bus bus = findBus(busRegistrationNumber);
        authorizeOperatorAccess(bus.getOperator().getCompanyName());

        if (!Boolean.TRUE.equals(bus.getIsActive())) {
            throw new IllegalArgumentException("Cannot add a seat to an inactive bus");
        }

        if (busSeatRepository.existsByBus_RegistrationNumberIgnoreCaseAndSeatNumberIgnoreCase(
                busRegistrationNumber, request.getSeatNumber())) {
            throw new IllegalArgumentException(
                    "Seat number '" + request.getSeatNumber() + "' already exists on this bus");
        }

        BusSeat busSeat = new BusSeat();
        busSeat.setBusSeatId(generateId());
        busSeat.setSeatNumber(request.getSeatNumber().trim());
        busSeat.setSeatPosition(request.getSeatPosition());
        busSeat.setIsActive(true);
        busSeat.setBus(bus);

        return convertToBusSeatDTO(busSeatRepository.save(busSeat));
    }


    @Override
    @Transactional(readOnly = true)
    public BusSeatDTO getBusSeat(String busRegistrationNumber, String seatNumber) {
        BusSeat busSeat = busSeatRepository
                .findByBus_RegistrationNumberIgnoreCaseAndSeatNumberIgnoreCase(busRegistrationNumber, seatNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Seat '" + seatNumber + "' not found on bus '" + busRegistrationNumber + "'"));
        return convertToBusSeatDTO(busSeat);
    }


    @Override
    @Transactional(readOnly = true)
    public List<BusSeatDTO> getSeatsByBus(String busRegistrationNumber) {
        findBus(busRegistrationNumber);
        return busSeatRepository.findByBus_RegistrationNumberIgnoreCase(busRegistrationNumber)
                .stream().map(this::convertToBusSeatDTO).toList();
    }


    @Override
    public BusSeatDTO updateBusSeat(String busRegistrationNumber, String seatNumber, BusSeatCreateRequest request) {
        BusSeat existingSeat = busSeatRepository
                .findByBus_RegistrationNumberIgnoreCaseAndSeatNumberIgnoreCase(busRegistrationNumber, seatNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Seat '" + seatNumber + "' not found on bus '" + busRegistrationNumber + "'"));

        authorizeOperatorAccess(existingSeat.getBus().getOperator().getCompanyName());

        if (!existingSeat.getSeatNumber().equalsIgnoreCase(request.getSeatNumber())
                && busSeatRepository.existsByBus_RegistrationNumberIgnoreCaseAndSeatNumberIgnoreCase(
                        busRegistrationNumber, request.getSeatNumber())) {
            throw new IllegalArgumentException(
                    "Seat number '" + request.getSeatNumber() + "' already exists on this bus");
        }

        existingSeat.setSeatNumber(request.getSeatNumber().trim());
        existingSeat.setSeatPosition(request.getSeatPosition());

        return convertToBusSeatDTO(busSeatRepository.save(existingSeat));
    }


    @Override
    public void deactivateBusSeat(String busRegistrationNumber, String seatNumber) {
        BusSeat busSeat = busSeatRepository
                .findByBus_RegistrationNumberIgnoreCaseAndSeatNumberIgnoreCase(busRegistrationNumber, seatNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Seat '" + seatNumber + "' not found on bus '" + busRegistrationNumber + "'"));

        authorizeOperatorAccess(busSeat.getBus().getOperator().getCompanyName());

        if (!Boolean.TRUE.equals(busSeat.getIsActive())) {
            throw new IllegalArgumentException("Seat is already inactive");
        }

        busSeat.setIsActive(false);
        busSeatRepository.save(busSeat);
    }


    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    private Bus findBus(String registrationNumber) {
        return busRepository.findByRegistrationNumberIgnoreCase(registrationNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Bus with registration number '" + registrationNumber + "' not found"));
    }


    private BusDTO convertToBusDTO(Bus bus) {
        BusDTO dto = new BusDTO();
        dto.setBusId(bus.getBusId());
        dto.setRegistrationNumber(bus.getRegistrationNumber());
        dto.setBusType(bus.getBusType());
        dto.setAmenities(bus.getAmenities());
        dto.setIsActive(bus.getIsActive());

        if (bus.getOperator() != null) {
            dto.setOperatorCompanyName(bus.getOperator().getCompanyName());
        }

        dto.setPetsAllowed(bus.getPetsAllowed());
        dto.setBaggagePolicy(bus.getBaggagePolicy());
        dto.setLastTripDate(bus.getLastTripDate());
        dto.setActivationRequestStatus(bus.getActivationRequestStatus());
        dto.setActivationRequestNote(bus.getActivationRequestNote());
        dto.setCompensationAmount(bus.getCompensationAmount());
        dto.setAdminRejectionNote(bus.getAdminRejectionNote());
        dto.setActivationRequestedAt(bus.getActivationRequestedAt());
        dto.setActivationApprovedAt(bus.getActivationApprovedAt());

        return dto;
    }


    private BusSeatDTO convertToBusSeatDTO(BusSeat busSeat) {
        BusSeatDTO dto = new BusSeatDTO();
        dto.setBusSeatId(busSeat.getBusSeatId());
        dto.setSeatNumber(busSeat.getSeatNumber());
        dto.setSeatPosition(busSeat.getSeatPosition());
        dto.setIsActive(busSeat.getIsActive());

        if (busSeat.getBus() != null) {
            dto.setBusRegistrationNumber(busSeat.getBus().getRegistrationNumber());
        }

        return dto;
    }


    private String generateId() {
        return EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_BUS);
    }

    private boolean isDestructiveChange(Bus existing, BusCreateRequest request) {
        boolean regChanged = !existing.getRegistrationNumber().equalsIgnoreCase(request.getRegistrationNumber());
        boolean typeChanged = existing.getBusType() != null && !existing.getBusType().equals(request.getBusType());
        return regChanged || typeChanged;
    }
}