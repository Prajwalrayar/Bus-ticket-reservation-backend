package com.crimsonlogic.busticketbooking.service;

import com.crimsonlogic.busticketbooking.dto.NotificationDTO;
import com.crimsonlogic.busticketbooking.entity.Booking;
import com.crimsonlogic.busticketbooking.entity.Cancellation;
import com.crimsonlogic.busticketbooking.entity.Notification;
import com.crimsonlogic.busticketbooking.entity.Payment;
import com.crimsonlogic.busticketbooking.enums.NotificationType;

import java.util.List;

public interface NotificationService {

    void createNotification(
            String userId,
            NotificationType notificationType,
            String title,
            String message
    );

    void sendBookingConfirmationNotification(
            Booking booking
    );

    void sendPaymentConfirmationNotification(
            Payment payment
    );

    void sendCancellationNotification(
            Cancellation cancellation
    );

    List<NotificationDTO> getMyNotifications();

    List<NotificationDTO> getMyUnreadNotifications();

    NotificationDTO markAsRead(
            String notificationId
    );

    void markAllAsRead();
}
