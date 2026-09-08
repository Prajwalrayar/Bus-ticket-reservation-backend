package com.crimsonlogic.busticketbooking.service.impl;

import com.crimsonlogic.busticketbooking.dto.ReviewCreateRequest;
import com.crimsonlogic.busticketbooking.dto.ReviewDTO;
import com.crimsonlogic.busticketbooking.entity.Booking;
import com.crimsonlogic.busticketbooking.entity.Review;
import com.crimsonlogic.busticketbooking.entity.User;
import com.crimsonlogic.busticketbooking.repository.BookingRepository;
import com.crimsonlogic.busticketbooking.repository.ReviewRepository;
import com.crimsonlogic.busticketbooking.repository.UserRepository;
import com.crimsonlogic.busticketbooking.service.ReviewService;
import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final EntityIdGenerator entityIdGenerator;


    // =========================================================
    // CREATE REVIEW
    // =========================================================

    @Override
    public ReviewDTO createReview(
            String bookingId,
            ReviewCreateRequest request) {

        /*
         * Get the currently authenticated user
         * from Spring Security.
         */
        User user = getAuthenticatedUser();

        /*
         * Find the booking.
         */
        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Booking not found"
                        )
                );

        /*
         * A review can only be written after the journey is completed.
         */
        LocalDateTime arrivalDateTime = LocalDateTime.of(
                booking.getTrip().getArrivalDate(),
                booking.getTrip().getArrivalTime()
        );

        if (arrivalDateTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "You can only review a journey after it has been completed."
            );
        }

        /*
         * Only the user who made the booking
         * can create its review.
         */
        if (!booking.getBookedByUser()
                .getUserId()
                .equals(user.getUserId())) {

            throw new IllegalArgumentException(
                    "You can only review your own booking"
            );
        }

        /*
         * One booking can have only one review.
         */
        if (reviewRepository
                .existsByBooking_BookingId(bookingId)) {

            throw new IllegalArgumentException(
                    "A review already exists for this booking"
            );
        }

        /*
         * Create review.
         */
        Review review = new Review();

        review.setReviewId(
                EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_REVIEW)
        );

        review.setRating(
                request.getRating()
        );

        review.setComment(
                request.getComment()
        );

        review.setUser(user);

        review.setBooking(booking);

        Review savedReview =
                reviewRepository.save(review);

        return convertToDTO(savedReview);
    }


    // =========================================================
    // GET REVIEW FOR A BOOKING
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public ReviewDTO getReviewByBooking(
            String bookingId) {

        User user = getAuthenticatedUser();

        /*
         * Make sure the booking exists.
         */
        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Booking not found"
                        )
                );

        /*
         * User can access the review only if
         * the booking belongs to them.
         */
        if (!booking.getBookedByUser()
                .getUserId()
                .equals(user.getUserId())) {

            throw new IllegalArgumentException(
                    "You can only access your own booking"
            );
        }

        Review review =
                reviewRepository
                        .findByBooking_BookingId(
                                bookingId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Review not found for this booking"
                                )
                        );

        return convertToDTO(review);
    }


    // =========================================================
    // GET MY REVIEWS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<ReviewDTO> getMyReviews() {

        User user = getAuthenticatedUser();

        return reviewRepository
                .findByUser_UserIdOrderByCreatedAtDesc(
                        user.getUserId()
                )
                .stream()
                .map(this::convertToDTO)
                .toList();
    }


    // =========================================================
    // UPDATE REVIEW
    // =========================================================

    @Override
    public ReviewDTO updateReview(
            String bookingId,
            ReviewCreateRequest request) {

        User user = getAuthenticatedUser();

        /*
         * Find review through the booking.
         */
        Review review =
                reviewRepository
                        .findByBooking_BookingId(
                                bookingId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Review not found for this booking"
                                )
                        );

        /*
         * Only the owner can update the review.
         */
        if (!review.getUser()
                .getUserId()
                .equals(user.getUserId())) {

            throw new IllegalArgumentException(
                    "You can only update your own review"
            );
        }

        review.setRating(
                request.getRating()
        );

        review.setComment(
                request.getComment()
        );

        Review updatedReview =
                reviewRepository.save(review);

        return convertToDTO(updatedReview);
    }


    // =========================================================
    // DELETE REVIEW
    // =========================================================

    @Override
    public void deleteReview(
            String bookingId) {

        User user = getAuthenticatedUser();

        /*
         * Find the review using the booking.
         */
        Review review =
                reviewRepository
                        .findByBooking_BookingId(
                                bookingId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Review not found for this booking"
                                )
                        );

        /*
         * Only the owner can delete the review.
         */
        if (!review.getUser()
                .getUserId()
                .equals(user.getUserId())) {

            throw new IllegalArgumentException(
                    "You can only delete your own review"
            );
        }

        reviewRepository.delete(review);
    }


    // =========================================================
    // GET AUTHENTICATED USER
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

        /*
         * Spring Security username is assumed
         * to be the user's email.
         */
        String email =
                authentication.getName();

        return userRepository
                .findByUserEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user not found"
                        )
                );
    }


    // =========================================================
    // ENTITY â†’ DTO
    // =========================================================

    private ReviewDTO convertToDTO(
            Review review) {

        ReviewDTO dto =
                new ReviewDTO();

        dto.setReviewId(
                review.getReviewId()
        );

        dto.setRating(
                review.getRating()
        );

        dto.setComment(
                review.getComment()
        );

        dto.setCreatedAt(
                review.getCreatedAt()
        );

        dto.setUpdatedAt(
                review.getUpdatedAt()
        );

        /*
         * ReviewDTO does not contain userId.
         * Only expose the user's display name.
         */
        if (review.getUser() != null) {

            dto.setUserName(
                    review.getUser()
                            .getUserName()
            );
        }

        /*
         * Booking ID is included in the response.
         */
        if (review.getBooking() != null) {

            dto.setBookingId(
                    review.getBooking()
                            .getBookingId()
            );
        }

        return dto;
    }
}