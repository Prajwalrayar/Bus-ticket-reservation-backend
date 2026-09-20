package com.crimsonlogic.busticketbooking.service.impl;

import com.crimsonlogic.busticketbooking.dto.CancellationDTO;
import com.crimsonlogic.busticketbooking.dto.CancellationRequest;
import com.crimsonlogic.busticketbooking.entity.Booking;
import com.crimsonlogic.busticketbooking.entity.Cancellation;
import com.crimsonlogic.busticketbooking.entity.User;
import com.crimsonlogic.busticketbooking.entity.UserRole;
import com.crimsonlogic.busticketbooking.enums.BookingStatus;
import com.crimsonlogic.busticketbooking.enums.RefundStatus;
import com.crimsonlogic.busticketbooking.repository.BookingRepository;
import com.crimsonlogic.busticketbooking.repository.CancellationRepository;
import com.crimsonlogic.busticketbooking.repository.UserRepository;
import com.crimsonlogic.busticketbooking.service.CancellationService;
import com.crimsonlogic.busticketbooking.service.WalletService;
import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.temporal.ChronoUnit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import org.json.JSONObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CancellationServiceImpl
        implements CancellationService {

    private final CancellationRepository cancellationRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final EntityIdGenerator entityIdGenerator;
    private final WalletService walletService;
    private final RazorpayClient razorpayClient;
    private final com.crimsonlogic.busticketbooking.repository.PaymentRepository paymentRepository;
    private final com.crimsonlogic.busticketbooking.service.NotificationService notificationService;


    // =========================================================
    // GET CANCELLATION ESTIMATE
    // =========================================================
    @Override
    @Transactional(readOnly = true)
    public com.crimsonlogic.busticketbooking.dto.CancellationEstimateDTO getCancellationEstimate(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        
        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Booking is already cancelled");
        }

        LocalDateTime departureTime = LocalDateTime.of(booking.getTrip().getTravelDate(), booking.getTrip().getDepartureTime());
        long minutesUntilDeparture = ChronoUnit.MINUTES.between(LocalDateTime.now(), departureTime);
        
        if (minutesUntilDeparture < 0) {
            throw new IllegalArgumentException("Your trip has already started you cannot cancel now");
        } else if (minutesUntilDeparture < 120) {
            throw new IllegalArgumentException("Your trip is about to start within 2 hours so you cannot cancel the booking");
        }
        
        long hoursUntilDeparture = ChronoUnit.HOURS.between(LocalDateTime.now(), departureTime);
        BigDecimal cancellationFeePercentage = BigDecimal.ZERO;
        String ruleApplied = "";
        
        if (hoursUntilDeparture > 24) {
            cancellationFeePercentage = new BigDecimal("0.10"); // 10% fee
            ruleApplied = "More than 24 hours before departure (10% fee)";
        } else if (hoursUntilDeparture > 12) {
            cancellationFeePercentage = new BigDecimal("0.50"); // 50% fee
            ruleApplied = "Between 12 and 24 hours before departure (50% fee)";
        } else {
            cancellationFeePercentage = new BigDecimal("1.00"); // 100% fee
            ruleApplied = "Less than 12 hours before departure (100% fee)";
        }

        BigDecimal cancellationFee = booking.getTotalAmount().multiply(cancellationFeePercentage);
        BigDecimal refundAmount = booking.getTotalAmount().subtract(cancellationFee);
        
        String paymentMethod = null;
        String paymentProvider = null;
        String refundDestination = null;
        
        com.crimsonlogic.busticketbooking.entity.Payment successfulPayment = booking.getPayments().stream()
                .filter(p -> p.getPaymentStatus() == com.crimsonlogic.busticketbooking.enums.PaymentStatus.SUCCESS)
                .findFirst()
                .orElse(null);
                
        if (successfulPayment != null) {
            paymentMethod = successfulPayment.getPaymentMethod();
            paymentProvider = successfulPayment.getPaymentProvider();
            
            if ("WALLET".equalsIgnoreCase(paymentMethod)) {
                refundDestination = "Wallet Balance";
            } else {
                refundDestination = "Original Payment Method";
                if (paymentProvider != null && !paymentProvider.isEmpty()) {
                    refundDestination += " (" + paymentProvider + ")";
                }
            }
        }

        return com.crimsonlogic.busticketbooking.dto.CancellationEstimateDTO.builder()
                .bookingId(bookingId)
                .totalAmount(booking.getTotalAmount())
                .cancellationFeePercentage(cancellationFeePercentage)
                .cancellationFee(cancellationFee)
                .refundAmount(refundAmount)
                .ruleApplied(ruleApplied)
                .paymentMethod(paymentMethod)
                .paymentProvider(paymentProvider)
                .refundDestination(refundDestination)
                .build();
    }

    // =========================================================
    // CANCEL BOOKING
    // =========================================================

    @Override
    public CancellationDTO cancelBooking(
            String bookingId,
            CancellationRequest request) {

        User currentUser = getAuthenticatedUser();

        /*
         * Find the booking.
         */
        Booking booking =
                bookingRepository.findById(bookingId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Booking not found"
                                )
                        );

        /*
         * Ownership check: only the booking owner or an ADMIN can cancel.
         */
        boolean isAdmin = currentUser.getUserRoles().stream()
                .map(UserRole::getRoleName)
                .anyMatch("ADMIN"::equals);

        if (!isAdmin && !booking.getBookedByUser().getUserId().equals(currentUser.getUserId())) {
            throw new IllegalArgumentException("Access denied: this booking does not belong to you");
        }


        /*
         * A booking can only be cancelled once.
         */
        if (booking.getBookingStatus()
                == BookingStatus.CANCELLED) {

            throw new IllegalArgumentException(
                    "Booking is already cancelled"
            );
        }


        /*
         * Check whether a cancellation record
         * already exists.
         */
        if (cancellationRepository
                .existsByBooking_BookingId(bookingId)) {

            throw new IllegalArgumentException(
                    "Cancellation already exists for this booking"
            );
        }


        /*
         * Check cancellation deadline when one
         * has been defined for the booking.
         */
        if (booking.getCancellationDeadline() != null
                && LocalDateTime.now()
                .isAfter(
                        booking.getCancellationDeadline()
                )) {

            throw new IllegalArgumentException(
                    "Cancellation deadline has passed"
            );
        }


        /*
         * The user performing the cancellation
         * is obtained from Spring Security context.
         */
        User cancelledByUser = currentUser;


        /*
         * Calculate cancellation fee dynamically based on departure time.
         */
        LocalDateTime departureTime = LocalDateTime.of(booking.getTrip().getTravelDate(), booking.getTrip().getDepartureTime());
        long minutesUntilDeparture = ChronoUnit.MINUTES.between(LocalDateTime.now(), departureTime);
        
        if (minutesUntilDeparture < 0) {
            throw new IllegalArgumentException("Your trip has already started you cannot cancel now");
        } else if (minutesUntilDeparture < 120) {
            throw new IllegalArgumentException("Your trip is about to start within 2 hours so you cannot cancel the booking");
        }
        
        long hoursUntilDeparture = ChronoUnit.HOURS.between(LocalDateTime.now(), departureTime);
        BigDecimal cancellationFeePercentage = BigDecimal.ZERO;
        
        if (hoursUntilDeparture > 24) {
            cancellationFeePercentage = new BigDecimal("0.10"); // 10% fee
        } else if (hoursUntilDeparture > 12) {
            cancellationFeePercentage = new BigDecimal("0.50"); // 50% fee
        } else {
            cancellationFeePercentage = new BigDecimal("1.00"); // 100% fee
        }

        BigDecimal cancellationFee = booking.getTotalAmount().multiply(cancellationFeePercentage);
        BigDecimal refundAmount = booking.getTotalAmount().subtract(cancellationFee);


        /*
         * Create cancellation record.
         */
        Cancellation cancellation =
                new Cancellation();

        cancellation.setCancellationId(
                EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_CANCELLATION)
        );

        cancellation.setCancellationReference(
                generateCancellationReference()
        );

        cancellation.setCancellationReason(
                request.getCancellationReason()
        );

        cancellation.setCancelledByUser(
                cancelledByUser
        );

        cancellation.setCancellationFee(
                cancellationFee
        );

        cancellation.setRefundAmount(
                refundAmount
        );

        /*
         * Refund integration is not implemented yet.
         *
         * We therefore record the refund as
         * INITIATED rather than falsely marking it
         * COMPLETED.
         */
        cancellation.setRefundStatus(
                refundAmount.signum() > 0
                        ? RefundStatus.INITIATED
                        : RefundStatus.NOT_APPLICABLE
        );

        cancellation.setCancelledAt(
                LocalDateTime.now()
        );

        cancellation.setBooking(
                booking
        );


        /*
         * Update booking status.
         */
        booking.setBookingStatus(
                BookingStatus.CANCELLED
        );

        /*
         * Release the seats back to AVAILABLE status.
         */
        if (booking.getBookingSeats() != null) {
            for (com.crimsonlogic.busticketbooking.entity.BookingSeat bs : booking.getBookingSeats()) {
                if (bs.getTripSeat() != null) {
                    bs.getTripSeat().setSeatStatus(com.crimsonlogic.busticketbooking.enums.SeatStatus.AVAILABLE);
                    bs.getTripSeat().setLockedByUserId(null);
                    bs.getTripSeat().setLockExpiryTime(null);
                }
            }
        }

        /*
         * Persist both changes in the same transaction.
         */
        bookingRepository.save(booking);

        Cancellation savedCancellation =
                cancellationRepository.save(
                        cancellation
                );


        return convertToDTO(
                savedCancellation
        );
    }


    // =========================================================
    // GET PENDING REFUNDS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public java.util.List<CancellationDTO> getPendingRefunds() {
        // Find cancellations that are in INITIATED, PROCESSING, or COMPLETED state
        java.util.List<Cancellation> cancellations = cancellationRepository
                .findByRefundStatusInOrderByCreatedAtDesc(
                        java.util.List.of(RefundStatus.INITIATED, RefundStatus.PROCESSING, RefundStatus.COMPLETED)
                );

        return cancellations.stream()
                .map(this::convertToDTO)
                .collect(java.util.stream.Collectors.toList());
    }


    // =========================================================
    // PROCESS REFUND
    // =========================================================

    @Override
    public CancellationDTO processRefund(
            String cancellationId) {

        Cancellation cancellation =
                cancellationRepository.findById(cancellationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Cancellation not found"
                                )
                        );

        if (cancellation.getRefundStatus() == RefundStatus.COMPLETED) {
            throw new IllegalArgumentException("Refund is already completed");
        }
        if (cancellation.getRefundStatus() == RefundStatus.NOT_APPLICABLE) {
            throw new IllegalArgumentException("Refund is not applicable for this cancellation");
        }

        BigDecimal refundAmount = cancellation.getRefundAmount();
        String generatedRefundRef = null;

        if (refundAmount != null && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            com.crimsonlogic.busticketbooking.entity.Payment payment = null;
            if (cancellation.getBooking().getPayments() != null) {
                payment = cancellation.getBooking().getPayments().stream()
                        .filter(p -> com.crimsonlogic.busticketbooking.enums.PaymentStatus.SUCCESS.equals(p.getPaymentStatus()))
                        .findFirst()
                        .orElse(null);
            }

            if (payment != null) {
                BigDecimal walletUsed = payment.getWalletAmountUsed() != null ? payment.getWalletAmountUsed() : BigDecimal.ZERO;
                BigDecimal gatewayAmount = payment.getPaymentAmount() != null ? payment.getPaymentAmount() : BigDecimal.ZERO;

                BigDecimal amountToRefundToGateway = BigDecimal.ZERO;
                BigDecimal amountToRefundToWallet = BigDecimal.ZERO;

                if (refundAmount.compareTo(gatewayAmount) <= 0) {
                    amountToRefundToGateway = refundAmount;
                } else {
                    amountToRefundToGateway = gatewayAmount;
                    amountToRefundToWallet = refundAmount.subtract(gatewayAmount);
                }

                if (amountToRefundToGateway.compareTo(BigDecimal.ZERO) > 0 && payment.getGatewayTransactionId() != null && payment.getGatewayTransactionId().startsWith("pay_")) {
                    try {
                        JSONObject refundRequest = new JSONObject();
                        refundRequest.put("amount", amountToRefundToGateway.multiply(new BigDecimal("100")).intValue());
                        refundRequest.put("speed", "optimum"); // Process instant refund if possible
                        Refund razorpayRefund = razorpayClient.payments.refund(payment.getGatewayTransactionId(), refundRequest);
                        generatedRefundRef = razorpayRefund.get("id");
                        log.info("Razorpay refund successful for booking {}, refundId: {}", cancellation.getBooking().getBookingId(), generatedRefundRef);
                    } catch (RazorpayException e) {
                        log.error("Razorpay refund failed", e);
                        throw new RuntimeException("Razorpay refund failed: " + e.getMessage());
                    }
                } else if (amountToRefundToGateway.compareTo(BigDecimal.ZERO) > 0) {
                    // Mock payment
                    generatedRefundRef = "rfnd_" + java.util.UUID.randomUUID().toString().substring(0, 8);
                }

                if (amountToRefundToWallet.compareTo(BigDecimal.ZERO) > 0) {
                    walletService.addBalance(
                            cancellation.getBooking().getBookedByUser().getUserId(),
                            amountToRefundToWallet,
                            "Refund for cancelled booking: " + cancellation.getBooking().getBookingId(),
                            generatedRefundRef != null ? generatedRefundRef : "WALLET_REFUND"
                    );
                    if (generatedRefundRef == null) {
                        generatedRefundRef = "WALLET_REFUND";
                    }
                }
            } else {
                 walletService.addBalance(
                            cancellation.getBooking().getBookedByUser().getUserId(),
                            refundAmount,
                            "Refund for cancelled booking: " + cancellation.getBooking().getBookingId(),
                            "WALLET_REFUND"
                 );
                 generatedRefundRef = "WALLET_REFUND";
            }
        }

        cancellation.setRefundStatus(RefundStatus.COMPLETED);
        cancellation.setRefundReference(generatedRefundRef);
        cancellation.setRefundCompletedAt(LocalDateTime.now());

        Cancellation savedCancellation = cancellationRepository.save(cancellation);
        
        // Send Notification
        try {
            String source = savedCancellation.getBooking().getTrip().getRoute().getSource();
            String destination = savedCancellation.getBooking().getTrip().getRoute().getDestination();
            String notificationMessage = String.format(
                    "Your refund of ₹%s is processed for booking %s from %s to %s and the amount is credited to your original payment method.",
                    refundAmount,
                    savedCancellation.getBooking().getBookingReference(),
                    source,
                    destination
            );

            notificationService.createNotification(
                    savedCancellation.getBooking().getBookedByUser().getUserId(),
                    com.crimsonlogic.busticketbooking.enums.NotificationType.REFUND_UPDATE,
                    "Refund Processed",
                    notificationMessage
            );
        } catch (Exception e) {
            log.warn("Failed to send refund notification for booking {}", savedCancellation.getBooking().getBookingId(), e);
        }

        return convertToDTO(savedCancellation);
    }


    // =========================================================
    // GET BY ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public CancellationDTO getCancellationById(
            String cancellationId) {

        Cancellation cancellation =
                cancellationRepository.findById(
                                cancellationId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Cancellation not found"
                                )
                        );

        return convertToDTO(
                cancellation
        );
    }


    // =========================================================
    // GET BY BOOKING
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public CancellationDTO getCancellationByBooking(
            String bookingId) {

        Cancellation cancellation =
                cancellationRepository
                        .findByBooking_BookingId(
                                bookingId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Cancellation for booking '"
                                                + bookingId
                                                + "' not found"
                                )
                        );

        return convertToDTO(
                cancellation
        );
    }


    // =========================================================
    // GET BY REFERENCE
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public CancellationDTO getCancellationByReference(
            String cancellationReference) {

        Cancellation cancellation =
                cancellationRepository
                        .findByCancellationReferenceIgnoreCase(
                                cancellationReference
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Cancellation with reference '"
                                                + cancellationReference
                                                + "' not found"
                                )
                        );

        return convertToDTO(
                cancellation
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


    // =========================================================
    // REFERENCE GENERATION
    // =========================================================

    private String generateCancellationReference() {

        return entityIdGenerator.generate(EntityIdGenerator.PREFIX_CANCELLATION, ref -> cancellationRepository.findByCancellationReferenceIgnoreCase(ref).isPresent());
    }


    // =========================================================
    // ENTITY â†’ DTO
    // =========================================================

    private CancellationDTO convertToDTO(Cancellation cancellation) {
        CancellationDTO dto = new CancellationDTO();
        dto.setCancellationId(cancellation.getCancellationId());
        dto.setCancellationReference(cancellation.getCancellationReference());
        dto.setCancellationReason(cancellation.getCancellationReason());
        dto.setCancellationFee(cancellation.getCancellationFee());
        dto.setRefundAmount(cancellation.getRefundAmount());
        dto.setRefundStatus(cancellation.getRefundStatus());
        dto.setRefundReference(cancellation.getRefundReference());
        dto.setCancelledAt(cancellation.getCancelledAt());
        dto.setRefundCompletedAt(cancellation.getRefundCompletedAt());
        
        if (cancellation.getBooking() != null) {
            dto.setBookingId(cancellation.getBooking().getBookingId());
            
            // Calculate and set ruleApplied
            LocalDateTime departureTime = LocalDateTime.of(cancellation.getBooking().getTrip().getTravelDate(), cancellation.getBooking().getTrip().getDepartureTime());
            long hoursUntilDeparture = ChronoUnit.HOURS.between(cancellation.getCancelledAt(), departureTime);
            String ruleApplied = "";
            if (hoursUntilDeparture > 24) {
                ruleApplied = "More than 24 hours before departure (10% fee)";
            } else if (hoursUntilDeparture > 12) {
                ruleApplied = "Between 12 and 24 hours before departure (50% fee)";
            } else {
                ruleApplied = "Less than 12 hours before departure (100% fee)";
            }
            dto.setRuleApplied(ruleApplied);
            
            // Populate payment details
            if (cancellation.getBooking().getPayments() != null) {
                com.crimsonlogic.busticketbooking.entity.Payment successfulPayment = cancellation.getBooking().getPayments().stream()
                        .filter(p -> com.crimsonlogic.busticketbooking.enums.PaymentStatus.SUCCESS.equals(p.getPaymentStatus()))
                        .findFirst()
                        .orElse(null);
                        
                if (successfulPayment != null) {
                    if (successfulPayment.getGatewayTransactionId() != null && !successfulPayment.getGatewayTransactionId().isEmpty()) {
                        dto.setPaymentTransactionId(successfulPayment.getGatewayTransactionId());
                    } else {
                        dto.setPaymentTransactionId(successfulPayment.getTransactionReference());
                    }
                    
                    dto.setPaymentMethod(successfulPayment.getPaymentMethod());
                    dto.setPaymentProvider(successfulPayment.getPaymentProvider());
                    
                    if ("WALLET".equalsIgnoreCase(successfulPayment.getPaymentMethod())) {
                        dto.setRefundDestination("Wallet Balance");
                    } else {
                        String dest = "Original Payment Method";
                        if (successfulPayment.getPaymentProvider() != null && !successfulPayment.getPaymentProvider().isEmpty()) {
                            dest += " (" + successfulPayment.getPaymentProvider() + ")";
                        }
                        dto.setRefundDestination(dest);
                    }
                }
            }
        }
        
        if (cancellation.getCancelledByUser() != null) {
            dto.setCancelledByUserId(cancellation.getCancelledByUser().getUserId());
        }
        return dto;
    }
}