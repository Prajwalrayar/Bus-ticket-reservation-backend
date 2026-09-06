package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.Payment;
import com.crimsonlogic.busticketbooking.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByTransactionReferenceIgnoreCase(
            String transactionReference
    );

    List<Payment> findByBooking_BookingId(
            String bookingId
    );

    List<Payment> findByBooking_BookingIdAndPaymentStatus(
            String bookingId,
            PaymentStatus paymentStatus
    );
}
