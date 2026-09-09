package com.crimsonlogic.busticketbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassengerAnalyticsDTO {
    private String passengerName;
    private Integer age;
    private String gender;
    private String seatNumber;
    private String boardingPoint;
    private String droppingPoint;
    private String bookingReference;
}
