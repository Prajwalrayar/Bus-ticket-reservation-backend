package com.crimsonlogic.busticketbooking.service.impl;

import com.crimsonlogic.busticketbooking.dto.PaymentDTO;
import com.crimsonlogic.busticketbooking.dto.PaymentRequest;
import com.crimsonlogic.busticketbooking.entity.Booking;
import com.crimsonlogic.busticketbooking.entity.Payment;
import com.crimsonlogic.busticketbooking.entity.TripSeat;
import com.crimsonlogic.busticketbooking.entity.Ticket;
import com.crimsonlogic.busticketbooking.enums.BookingStatus;
import com.crimsonlogic.busticketbooking.enums.PaymentStatus;
import com.crimsonlogic.busticketbooking.enums.SeatStatus;
import com.crimsonlogic.busticketbooking.repository.BookingRepository;
import com.crimsonlogic.busticketbooking.repository.PaymentRepository;
import com.crimsonlogic.busticketbooking.repository.TicketRepository;
import com.crimsonlogic.busticketbooking.repository.TripSeatRepository;
import com.crimsonlogic.busticketbooking.service.BookingService;
import com.crimsonlogic.busticketbooking.service.PaymentService;
import com.crimsonlogic.busticketbooking.service.WalletService;
import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.crimsonlogic.busticketbooking.dto.RazorpayOrderResponse;
import com.crimsonlogic.busticketbooking.dto.RazorpayVerificationRequest;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final TripSeatRepository tripSeatRepository;
    private final TicketRepository ticketRepository;
    private final EntityIdGenerator entityIdGenerator;
    private final WalletService walletService;
    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpaySecret;

    private static final Random RANDOM = new Random();

    // ── INITIATE (creates INITIATED record) ───────────────────────────────────

    @Override
    public PaymentDTO initiatePayment(String bookingId, PaymentRequest request) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (booking.getBookingStatus() == BookingStatus.CANCELLED || booking.getBookingStatus() == BookingStatus.FAILED) {
            throw new IllegalArgumentException("Cannot pay for a cancelled or failed booking");
        }

        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Booking is no longer pending");
        }

        if (booking.getExpiryTime() != null && booking.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Booking payment window has expired");
        }

        boolean alreadyPaid = paymentRepository
                .findByBooking_BookingIdAndPaymentStatus(bookingId, PaymentStatus.SUCCESS)
                .stream().findFirst().isPresent();
        if (alreadyPaid) {
            throw new IllegalArgumentException("Booking already paid");
        }

        BigDecimal walletAmountUsed = BigDecimal.ZERO;
        BigDecimal gatewayAmount = booking.getTotalAmount();
        String paymentMethod = request.getPaymentMethod();

        if (request.isUseWallet()) {
            BigDecimal walletBalance = walletService.getMyWallet().getBalance();
            if (walletBalance.compareTo(BigDecimal.ZERO) > 0) {
                if (walletBalance.compareTo(gatewayAmount) >= 0) {
                    walletAmountUsed = gatewayAmount;
                    gatewayAmount = BigDecimal.ZERO;
                    paymentMethod = "WALLET";
                } else {
                    walletAmountUsed = walletBalance;
                    gatewayAmount = gatewayAmount.subtract(walletBalance);
                }
            }
        }

        Payment payment = buildPayment(booking, paymentMethod, PaymentStatus.INITIATED, gatewayAmount, walletAmountUsed);
        Payment saved = paymentRepository.save(payment);
        return convertToDTO(saved);
    }

    // ── MOCK CHECKOUT (simulates gateway + confirms booking) ──────────────────

    @Override
    public PaymentDTO mockCheckout(String bookingId, PaymentRequest request) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (booking.getBookingStatus() == BookingStatus.CANCELLED || booking.getBookingStatus() == BookingStatus.FAILED) {
            throw new IllegalArgumentException("Cannot pay for a cancelled or failed booking");
        }

        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Booking is no longer pending");
        }

        if (booking.getExpiryTime() != null && booking.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Booking payment window has expired");
        }

        // 80% success simulation (100% success if using WALLET only)
        boolean success = RANDOM.nextInt(10) < 8;
        
        BigDecimal walletAmountUsed = BigDecimal.ZERO;
        BigDecimal gatewayAmount = booking.getTotalAmount();
        String paymentMethod = request.getPaymentMethod();

        if (request.isUseWallet()) {
            BigDecimal walletBalance = walletService.getMyWallet().getBalance();
            if (walletBalance.compareTo(BigDecimal.ZERO) > 0) {
                if (walletBalance.compareTo(gatewayAmount) >= 0) {
                    walletAmountUsed = gatewayAmount;
                    gatewayAmount = BigDecimal.ZERO;
                    paymentMethod = "WALLET";
                    success = true; // Wallet-only is always successful
                } else {
                    walletAmountUsed = walletBalance;
                    gatewayAmount = gatewayAmount.subtract(walletBalance);
                }
            }
        }

        Payment payment = buildPayment(booking, paymentMethod,
                success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED, gatewayAmount, walletAmountUsed);
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

            // Deduct from wallet if used
            if (walletAmountUsed.compareTo(BigDecimal.ZERO) > 0) {
                walletService.deductBalance(booking.getBookedByUser().getUserId(), walletAmountUsed, bookingId);
            }

            // ── Generate Ticket ──────────────────────────────────────────────
            Ticket ticket = new Ticket();
            ticket.setBooking(booking);
            ticket.setIssuedAt(LocalDateTime.now());
            ticket.setTicketNumber("TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            ticket.setVerificationCode(UUID.randomUUID().toString());
            ticketRepository.save(ticket);

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

    // ── RAZORPAY INTEGRATION ──────────────────────────────────────────────────

    @Override
    public RazorpayOrderResponse createRazorpayOrder(String bookingId, PaymentRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Booking is no longer pending");
        }
        if (booking.getExpiryTime() != null && booking.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Booking payment window has expired");
        }

        BigDecimal walletAmountUsed = BigDecimal.ZERO;
        BigDecimal gatewayAmount = booking.getTotalAmount();
        
        if (request.isUseWallet()) {
            BigDecimal walletBalance = walletService.getMyWallet().getBalance();
            if (walletBalance.compareTo(BigDecimal.ZERO) > 0) {
                if (walletBalance.compareTo(gatewayAmount) >= 0) {
                    walletAmountUsed = gatewayAmount;
                    gatewayAmount = BigDecimal.ZERO;
                } else {
                    walletAmountUsed = walletBalance;
                    gatewayAmount = gatewayAmount.subtract(walletBalance);
                }
            }
        }

        Payment payment = buildPayment(booking, "RAZORPAY", PaymentStatus.INITIATED, gatewayAmount, walletAmountUsed);
        payment = paymentRepository.save(payment);

        RazorpayOrderResponse response = new RazorpayOrderResponse();
        response.setPayment(convertToDTO(payment));

        if (gatewayAmount.compareTo(BigDecimal.ZERO) > 0) {
            try {
                JSONObject orderRequest = new JSONObject();
                // amount in paise
                orderRequest.put("amount", gatewayAmount.multiply(new BigDecimal("100")).intValue());
                orderRequest.put("currency", "INR");
                orderRequest.put("receipt", payment.getPaymentId());

                Order razorpayOrder = razorpayClient.orders.create(orderRequest);
                
                response.setOrderId(razorpayOrder.get("id"));
                response.setKeyId(razorpayKeyId);
                response.setAmount(gatewayAmount);
                response.setCurrency("INR");
                
                payment.setGatewayTransactionId(razorpayOrder.get("id"));
                paymentRepository.save(payment);
            } catch (RazorpayException e) {
                log.error("Failed to create Razorpay Order", e);
                throw new RuntimeException("Failed to create Razorpay Order: " + e.getMessage());
            }
        }

        return response;
    }

    @Override
    public PaymentDTO verifyRazorpayPayment(String bookingId, RazorpayVerificationRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        
        Payment payment = paymentRepository.findByBooking_BookingId(bookingId).stream()
                .filter(p -> p.getGatewayTransactionId() != null && p.getGatewayTransactionId().equals(request.getRazorpayOrderId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Payment record not found for this order ID"));

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            boolean isValid = Utils.verifyPaymentSignature(options, razorpaySecret);

            if (isValid) {
                payment.setPaymentStatus(PaymentStatus.SUCCESS);
                payment.setPaymentCompletedAt(LocalDateTime.now());
                payment.setGatewayTransactionId(request.getRazorpayPaymentId());
                
                booking.setBookingStatus(BookingStatus.CONFIRMED);
                
                if (booking.getBookingSeats() != null) {
                    booking.getBookingSeats().forEach(bs -> {
                        TripSeat ts = bs.getTripSeat();
                        ts.setSeatStatus(SeatStatus.BOOKED);
                        ts.setLockedByUserId(null);
                        ts.setLockExpiryTime(null);
                        tripSeatRepository.save(ts);
                    });
                }
                
                if (payment.getWalletAmountUsed() != null && payment.getWalletAmountUsed().compareTo(BigDecimal.ZERO) > 0) {
                    walletService.deductBalance(booking.getBookedByUser().getUserId(), payment.getWalletAmountUsed(), bookingId);
                }
                
                Ticket ticket = new Ticket();
                ticket.setBooking(booking);
                ticket.setIssuedAt(LocalDateTime.now());
                ticket.setTicketNumber("TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                ticket.setVerificationCode(UUID.randomUUID().toString());
                ticketRepository.save(ticket);
                
                log.info("Razorpay Payment verified for booking {}. PNR: {}", bookingId, booking.getBookingReference());
            } else {
                failPaymentAndReleaseSeats(payment, booking, "Signature Verification Failed", "INVALID_SIGNATURE");
            }

        } catch (Exception e) {
            log.error("Razorpay signature verification failed", e);
            failPaymentAndReleaseSeats(payment, booking, "Verification Exception: " + e.getMessage(), "VERIFY_ERROR");
        }

        bookingRepository.save(booking);
        Payment saved = paymentRepository.save(payment);
        return convertToDTO(saved);
    }

    private void failPaymentAndReleaseSeats(Payment payment, Booking booking, String reason, String code) {
        payment.setPaymentStatus(PaymentStatus.FAILED);
        payment.setPaymentCompletedAt(LocalDateTime.now());
        payment.setFailureReason(reason);
        payment.setFailureCode(code);
        
        booking.setBookingStatus(BookingStatus.FAILED);

        log.warn("Payment FAILED for booking {}. Releasing seat locks. Reason: {}", booking.getBookingId(), reason);
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

    private Payment buildPayment(Booking booking, String method, PaymentStatus status, BigDecimal gatewayAmount, BigDecimal walletAmountUsed) {
        Payment p = new Payment();
        p.setPaymentId(entityIdGenerator.generate(EntityIdGenerator.PREFIX_PAYMENT, paymentRepository::existsById));
        p.setTransactionReference(entityIdGenerator.generate(EntityIdGenerator.PREFIX_PAYMENT,
                ref -> paymentRepository.findByTransactionReferenceIgnoreCase(ref).isPresent()));
        p.setPaymentMethod(method);
        p.setPaymentStatus(status);
        p.setPaymentAmount(gatewayAmount);
        p.setWalletAmountUsed(walletAmountUsed);
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
        dto.setWalletAmountUsed(payment.getWalletAmountUsed());
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