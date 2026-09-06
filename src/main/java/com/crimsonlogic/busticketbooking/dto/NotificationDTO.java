package com.crimsonlogic.busticketbooking.dto;

import com.crimsonlogic.busticketbooking.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {

    private String notificationId;

    private NotificationType notificationType;

    private String title;

    private String message;

    private Boolean isRead;

    private LocalDateTime readAt;

    private LocalDateTime createdAt;
}