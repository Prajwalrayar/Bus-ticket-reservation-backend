package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, String> {

    Optional<Review> findByBooking_BookingId(String bookingId);

    List<Review> findByUser_UserIdOrderByCreatedAtDesc(
            String userId
    );

    boolean existsByBooking_BookingId(String bookingId);

    List<Review> findBySentimentStatusIsNull();

    long countBySentimentStatus(com.crimsonlogic.busticketbooking.enums.SentimentStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT AVG(r.sentimentScore) FROM Review r")
    Double getAverageSentimentScore();
}