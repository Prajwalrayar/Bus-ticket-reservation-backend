package com.crimsonlogic.busticketbooking.service.impl;

import com.crimsonlogic.busticketbooking.dto.PaymentDTO;
import com.crimsonlogic.busticketbooking.dto.PaymentRequest;
import com.crimsonlogic.busticketbooking.entity.Booking;
import com.crimsonlogic.busticketbooking.entity.Payment;
import com.crimsonlogic.busticketbooking.entity.TripSeat;
import com.crimsonlogic.busticketbooking.enums.BookingStatus;
import com.crimsonlogic.busticketbooking.enums.PaymentStatus;
import com.crimsonlogic.busticketbooking.enums.SeatStatus;
import com.crimsonlogic.busticketbooking.repository.BookingRepository;
import com.crimsonlogic.busticketbooking.repository.PaymentRepository;
import com.crimsonlogic.busticketbooking.repository.TripSeatRepository;
import com.crimsonlogic.busticketbooking.service.PaymentService;
import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final TripSeatRepository tripSeatRepository;
    private final EntityIdGenerator entityIdGenerator;

    private static final Random RANDOM = new Random();

    // ── INITIATE (creates INITIATED record) ───────────────────────────────────

    @Override
    public PaymentDTO initiatePayment(String bookingId, PaymentRequest request) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot pay for a cancelled booking");
        }

        boolean alreadyPaid = paymentRepository
                .findByBooking_BookingIdAndPaymentStatus(bookingId, PaymentStatus.SUCCESS)
                .stream().findFirst().isPresent();
        if (alreadyPaid) {
            throw new IllegalArgumentException("Booking already paid");
        }

        Payment payment = buildPayment(booking, request.getPaymentMethod(), PaymentStatus.INITIATED);
        Payment saved = paymentRepository.save(payment);
        return convertToDTO(saved);
    }

    // ── MOCK CHECKOUT (simulates gateway + confirms booking) ──────────────────

    @Override
    public PaymentDTO mockCheckout(String bookingId, PaymentRequest request) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot pay for a cancelled booking");
        }

        // 80% success simulation
        boolean success = RANDOM.nextInt(10) < 8;

        Payment payment = buildPayment(booking, request.getPaymentMethod(),
                success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        payment.setPaymentCompletedAt(LocalDateTime.now());

        if (success) {
            // ── Confirm booking ──────────────────────────────────────────────
            booking.setBookingStatus(BookingStatus.CONFIRMED);
            payment.setGatewayTransactionId("GTX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            log.info("Payment SUCCESS for booking {}. PNR: {}", bookingId, booking.getBookingReference());

            // ── Mark all booked seats as BOOKED ─────────────────────────────
            if (booking.getBookingSeats() != null) {
                booking.getBookingSeats().forEach(bs -> {
                    TripSeat ts = bs.getTripSeat();
                    ts.setSeatStatus(SeatStatus.BOOKED);
                    ts.setLockedByUserId(null);
                    ts.setLockExpiryTime(null);
                    tripSeatRepository.save(ts);
                });
            }

        } else {
            // ── Release seat locks on payment failure ────────────────────────
            payment.setFailureReason("Payment declined by gateway");
            payment.setFailureCode("DECLINED");
            log.warn("Payment FAILED for booking {}. Releasing seat locks.", bookingId);

            if (booking.getBookingSeats() != null) {
                booking.getBookingSeats().forEach(bs -> {
                    TripSeat ts = bs.getTripSeat();
                    if (ts.getSeatStatus() == SeatStatus.TEMPORARILY_LOCKED) {
                        ts.setSeatStatus(SeatStatus.AVAILABLE);
                        ts.setLockedByUserId(null);
                        ts.setLockExpiryTime(null);
                        tripSeatRepository.save(ts);
                    }
                });
            }
        }

        bookingRepository.save(booking);
        Payment saved = paymentRepository.save(payment);
        return convertToDTO(saved);
    }

    // ── QUERY METHODS ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PaymentDTO getPaymentById(String paymentId) {
        return convertToDTO(paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found")));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDTO getPaymentByTransactionReference(String ref) {
        return convertToDTO(paymentRepository.findByTransactionReferenceIgnoreCase(ref)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for ref: " + ref)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentDTO> getPaymentsByBooking(String bookingId) {
        bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        return paymentRepository.findByBooking_BookingId(bookingId)
                .stream().map(this::convertToDTO).toList();
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private Payment buildPayment(Booking booking, String method, PaymentStatus status) {
        Payment p = new Payment();
        p.setPaymentId(entityIdGenerator.generate(EntityIdGenerator.PREFIX_PAYMENT, paymentRepository::existsById));
        p.setTransactionReference(entityIdGenerator.generate(EntityIdGenerator.PREFIX_PAYMENT,
                ref -> paymentRepository.findByTransactionReferenceIgnoreCase(ref).isPresent()));
        p.setPaymentMethod(method);
        p.setPaymentStatus(status);
        p.setPaymentAmount(booking.getTotalAmount());
        p.setPaymentInitiatedAt(LocalDateTime.now());
        p.setBooking(booking);
        return p;
    }

    private PaymentDTO convertToDTO(Payment payment) {
        PaymentDTO dto = new PaymentDTO();
        dto.setPaymentId(payment.getPaymentId());
        dto.setTransactionReference(payment.getTransactionReference());
        dto.setGatewayTransactionId(payment.getGatewayTransactionId());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setPaymentStatus(payment.getPaymentStatus());
        dto.setPaymentAmount(payment.getPaymentAmount());
        dto.setFailureReason(payment.getFailureReason());
        dto.setPaymentInitiatedAt(payment.getPaymentInitiatedAt());
        dto.setPaymentCompletedAt(payment.getPaymentCompletedAt());
        dto.setRefundInitiatedAt(payment.getRefundInitiatedAt());
        dto.setRefundCompletedAt(payment.getRefundCompletedAt());
        dto.setRefundReference(payment.getRefundReference());
        if (payment.getBooking() != null) {
            dto.setBookingId(payment.getBooking().getBookingId());
            dto.setBookingReference(payment.getBooking().getBookingReference());
        }
        return dto;
    }
}