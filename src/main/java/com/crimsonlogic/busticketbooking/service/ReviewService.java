package com.crimsonlogic.busticketbooking.service;

import com.crimsonlogic.busticketbooking.dto.ReviewCreateRequest;
import com.crimsonlogic.busticketbooking.dto.ReviewDTO;

import java.util.List;

public interface ReviewService {

    ReviewDTO createReview(
            String bookingId,
            ReviewCreateRequest request
    );

    ReviewDTO getReviewByBooking(
            String bookingId
    );

    List<ReviewDTO> getMyReviews();

    ReviewDTO updateReview(
            String bookingId,
            ReviewCreateRequest request
    );

    void deleteReview(
            String bookingId
    );
}