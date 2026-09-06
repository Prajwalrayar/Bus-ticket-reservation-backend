package com.crimsonlogic.busticketbooking.dto;

import com.crimsonlogic.busticketbooking.enums.BusType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketDTO {

    private String ticketId;

    private String ticketNumber;

    private String verificationCode;

    private String bookingId;

    private String operatorName;

    private String busNumber;

    private BusType busType;

    private String source;

    private String destination;

    private LocalDate travelDate;

    private LocalTime departureTime;

    private LocalTime arrivalTime;

    private LocalDateTime issuedAt;

    private String boardingPoint;

    private LocalTime boardingTime;

    private String droppingPoint;

    private List<String> seatNumbers;

    private Integer totalSeats;

    private BigDecimal totalAmount;

    private Boolean isValidated;

    private LocalDateTime validatedAt;
}