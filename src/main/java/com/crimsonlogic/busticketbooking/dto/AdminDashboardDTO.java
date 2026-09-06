package com.crimsonlogic.busticketbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardDTO {

    private long totalUsers;

    private long activeUsers;

    private long totalBuses;

    private long activeBuses;

    private long totalBookings;

    private long confirmedBookings;

    private long cancelledBookings;
}