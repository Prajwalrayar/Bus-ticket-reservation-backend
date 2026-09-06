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
import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class CancellationServiceImpl
        implements CancellationService {

    private final CancellationRepository cancellationRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final EntityIdGenerator entityIdGenerator;


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
         * Calculate cancellation fee.
         *
         * The actual cancellation policy can be
         * made configurable later.
         *
         * For now:
         * cancellation fee = 0
         * refund = full booking amount
         */
        BigDecimal cancellationFee =
                BigDecimal.ZERO;

        BigDecimal refundAmount =
                booking.getTotalAmount()
                        .subtract(cancellationFee);


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

    private CancellationDTO convertToDTO(
            Cancellation cancellation) {

        CancellationDTO dto =
                new CancellationDTO();

        dto.setCancellationId(
                cancellation.getCancellationId()
        );

        dto.setCancellationReference(
                cancellation.getCancellationReference()
        );

        dto.setCancellationReason(
                cancellation.getCancellationReason()
        );

        dto.setCancellationFee(
                cancellation.getCancellationFee()
        );

        dto.setRefundAmount(
                cancellation.getRefundAmount()
        );

        dto.setRefundStatus(
                cancellation.getRefundStatus()
        );

        dto.setRefundReference(
                cancellation.getRefundReference()
        );

        dto.setCancelledAt(
                cancellation.getCancelledAt()
        );

        dto.setRefundCompletedAt(
                cancellation.getRefundCompletedAt()
        );


        if (cancellation.getBooking() != null) {

            dto.setBookingId(
                    cancellation.getBooking()
                            .getBookingId()
            );
        }


        if (cancellation.getCancelledByUser() != null) {

            dto.setCancelledByUserId(
                    cancellation.getCancelledByUser()
                            .getUserId()
            );
        }

        return dto;
    }
}