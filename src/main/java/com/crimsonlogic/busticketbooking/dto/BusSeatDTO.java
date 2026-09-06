package com.crimsonlogic.busticketbooking.dto;

import com.crimsonlogic.busticketbooking.enums.SeatPosition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusSeatDTO {

    private String busSeatId;

    private String seatNumber;

    private SeatPosition seatPosition;

    private Boolean isActive;

    private String busRegistrationNumber;
}