package com.crimsonlogic.busticketbooking.dto;

import com.crimsonlogic.busticketbooking.enums.SeatPosition;
import com.crimsonlogic.busticketbooking.enums.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripSeatDTO {

    private String tripSeatId;

    private String seatNumber;

    private SeatPosition seatPosition;

    private SeatStatus seatStatus;

    private BigDecimal seatFare;

    private LocalDateTime lockExpiryTime;
}