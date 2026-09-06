package com.crimsonlogic.busticketbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingSeatDTO {

    private String bookingSeatId;

    private String seatNumber;

    private String passengerName;

    private Integer passengerAge;

    private String passengerGender;

    private String idType;

    private String idNumber;

    private String contactNumber;

    private BigDecimal seatFare;

    private Boolean isPrimary;
}
