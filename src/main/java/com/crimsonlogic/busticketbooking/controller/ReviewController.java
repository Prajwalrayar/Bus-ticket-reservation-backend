package com.crimsonlogic.busticketbooking.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.dto.ReviewCreateRequest;
import com.crimsonlogic.busticketbooking.dto.ReviewDTO;
import com.crimsonlogic.busticketbooking.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;


    // =========================================================
    // CREATE REVIEW
    // =========================================================

    @PostMapping("/{bookingId}/review")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewDTO>>
    createReview(
            @PathVariable String bookingId,
            @Valid @RequestBody ReviewCreateRequest request) {

        ReviewDTO review =
                reviewService.createReview(
                        bookingId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Review submitted successfully",
                                review
                        )
                );
    }


    // =========================================================
    // GET REVIEW FOR BOOKING
    // =========================================================

    @GetMapping("/{bookingId}/review")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewDTO>>
    getReviewByBooking(
            @PathVariable String bookingId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        reviewService.getReviewByBooking(
                                bookingId
                        )
                )
        );
    }



    // =========================================================
    // GET MY REVIEWS
    // =========================================================

    @GetMapping("/reviews/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ReviewDTO>>>
    getMyReviews() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        reviewService.getMyReviews()
                )
        );
    }


    // =========================================================
    // UPDATE REVIEW
    // =========================================================

    @PutMapping("/{bookingId}/review")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewDTO>>
    updateReview(
            @PathVariable String bookingId,
            @Valid @RequestBody ReviewCreateRequest request) {

        ReviewDTO review =
                reviewService.updateReview(
                        bookingId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Review updated successfully",
                        review
                )
        );
    }


    // =========================================================
    // DELETE REVIEW
    // =========================================================

    @DeleteMapping("/{bookingId}/review")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>>
    deleteReview(
            @PathVariable String bookingId) {

        reviewService.deleteReview(
                bookingId
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Review deleted successfully",
                        null
                )
        );
    }


}