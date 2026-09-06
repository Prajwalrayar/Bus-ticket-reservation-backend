package com.crimsonlogic.busticketbooking.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.dto.NotificationDTO;
import com.crimsonlogic.busticketbooking.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;


    // =========================================================
    // GET MY NOTIFICATIONS
    // =========================================================

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationDTO>>>
    getMyNotifications() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        notificationService.getMyNotifications()
                )
        );
    }


    // =========================================================
    // GET MY UNREAD NOTIFICATIONS
    // =========================================================

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>>
    getMyUnreadNotifications() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        notificationService.getMyUnreadNotifications()
                )
        );
    }


    // =========================================================
    // MARK ONE NOTIFICATION AS READ
    // =========================================================

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationDTO>>
    markAsRead(
            @PathVariable String notificationId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        notificationService.markAsRead(
                                notificationId
                        )
                )
        );
    }


    // =========================================================
    // MARK ALL NOTIFICATIONS AS READ
    // =========================================================

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>>
    markAllAsRead() {

        notificationService.markAllAsRead();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "All notifications marked as read",
                        null
                )
        );
    }
}