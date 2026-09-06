package com.crimsonlogic.busticketbooking.service.impl;

import com.crimsonlogic.busticketbooking.dto.NotificationDTO;
import com.crimsonlogic.busticketbooking.entity.Booking;
import com.crimsonlogic.busticketbooking.entity.Cancellation;
import com.crimsonlogic.busticketbooking.entity.Notification;
import com.crimsonlogic.busticketbooking.entity.Payment;
import com.crimsonlogic.busticketbooking.entity.User;
import com.crimsonlogic.busticketbooking.enums.NotificationType;
import com.crimsonlogic.busticketbooking.repository.NotificationRepository;
import com.crimsonlogic.busticketbooking.repository.UserRepository;
import com.crimsonlogic.busticketbooking.service.NotificationService;
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
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EntityIdGenerator entityIdGenerator;


    // =========================================================
    // CREATE NOTIFICATION
    // =========================================================

    /*
     * Internal service method.
     *
     * Other services such as BookingService,
     * PaymentService and CancellationService can call this.
     */
    @Override
    public void createNotification(
            String userId,
            NotificationType notificationType,
            String title,
            String message) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        Notification notification = new Notification();

        notification.setNotificationId(
                EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_NOTIFICATION)
        );

        notification.setNotificationType(
                notificationType
        );

        notification.setTitle(title);

        notification.setMessage(message);

        notification.setIsRead(false);

        notification.setUser(user);

        notificationRepository.save(notification);
    }


    // =========================================================
    // BOOKING CONFIRMATION
    // =========================================================

    @Override
    public void sendBookingConfirmationNotification(
            Booking booking) {

        User user = booking.getBookedByUser();

        createNotification(
                user.getUserId(),
                NotificationType.BOOKING_CONFIRMATION,
                "Booking Confirmed",
                "Your booking "
                        + booking.getBookingReference()
                        + " has been confirmed successfully."
        );
    }


    // =========================================================
    // PAYMENT CONFIRMATION
    // =========================================================

    @Override
    public void sendPaymentConfirmationNotification(
            Payment payment) {

        Booking booking = payment.getBooking();

        User user = booking.getBookedByUser();

        createNotification(
                user.getUserId(),
                NotificationType.PAYMENT_CONFIRMATION,
                "Payment Successful",
                "Payment for booking "
                        + booking.getBookingReference()
                        + " was successful."
        );
    }


    // =========================================================
    // CANCELLATION NOTIFICATION
    // =========================================================

    @Override
    public void sendCancellationNotification(
            Cancellation cancellation) {

        Booking booking = cancellation.getBooking();

        /*
         * The notification should go to the booking owner,
         * not necessarily the user who performed cancellation.
         *
         * Example:
         * Admin cancels a passenger's booking.
         * The passenger should receive the notification.
         */
        User user = booking.getBookedByUser();

        createNotification(
                user.getUserId(),
                NotificationType.CANCELLATION_UPDATE,
                "Booking Cancelled",
                "Your booking "
                        + booking.getBookingReference()
                        + " has been cancelled."
        );
    }


    // =========================================================
    // GET MY NOTIFICATIONS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getMyNotifications() {

        User user = getAuthenticatedUser();

        return notificationRepository
                .findByUser_UserIdOrderByCreatedAtDesc(
                        user.getUserId()
                )
                .stream()
                .map(this::convertToDTO)
                .toList();
    }


    // =========================================================
    // GET MY UNREAD NOTIFICATIONS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getMyUnreadNotifications() {

        User user = getAuthenticatedUser();

        return notificationRepository
                .findByUser_UserIdAndIsReadFalseOrderByCreatedAtDesc(
                        user.getUserId()
                )
                .stream()
                .map(this::convertToDTO)
                .toList();
    }


    // =========================================================
    // MARK ONE NOTIFICATION AS READ
    // =========================================================

    @Override
    public NotificationDTO markAsRead(
            String notificationId) {

        User user = getAuthenticatedUser();

        Notification notification =
                notificationRepository.findById(notificationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Notification not found"
                                )
                        );

        /*
         * Security/business rule:
         *
         * A user can mark only their own notification
         * as read.
         */
        if (!notification.getUser()
                .getUserId()
                .equals(user.getUserId())) {

            throw new IllegalArgumentException(
                    "Notification does not belong to this user"
            );
        }

        /*
         * Marking an already-read notification is harmless.
         */
        if (!Boolean.TRUE.equals(
                notification.getIsRead())) {

            notification.setIsRead(true);

            notification.setReadAt(
                    LocalDateTime.now()
            );
        }

        return convertToDTO(notification);
    }


    // =========================================================
    // MARK ALL MY NOTIFICATIONS AS READ
    // =========================================================

    @Override
    public void markAllAsRead() {

        User user = getAuthenticatedUser();

        List<Notification> notifications =
                notificationRepository
                        .findByUser_UserIdAndIsReadFalseOrderByCreatedAtDesc(
                                user.getUserId()
                        );

        if (notifications.isEmpty()) {
            return;
        }

        LocalDateTime readAt =
                LocalDateTime.now();

        for (Notification notification : notifications) {

            notification.setIsRead(true);

            notification.setReadAt(readAt);
        }

        notificationRepository.saveAll(
                notifications
        );
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

        String username =
                authentication.getName();

        /*
         * Change findByUserEmail() only if your application
         * uses another field as the login username.
         */
        return userRepository
                .findByUserEmail(username)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Authenticated user not found"
                        )
                );
    }


    // =========================================================
    // ENTITY â†’ DTO
    // =========================================================

    private NotificationDTO convertToDTO(
            Notification notification) {

        NotificationDTO dto =
                new NotificationDTO();

        dto.setNotificationId(
                notification.getNotificationId()
        );

        dto.setNotificationType(
                notification.getNotificationType()
        );

        dto.setTitle(
                notification.getTitle()
        );

        dto.setMessage(
                notification.getMessage()
        );

        dto.setIsRead(
                notification.getIsRead()
        );

        dto.setReadAt(
                notification.getReadAt()
        );

        dto.setCreatedAt(
                notification.getCreatedAt()
        );

        return dto;
    }
}