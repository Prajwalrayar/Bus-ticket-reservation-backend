package com.crimsonlogic.busticketbooking.dto;

import com.crimsonlogic.busticketbooking.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDTO {

    private String bookingId;

    private String bookingReference;

    private BookingStatus bookingStatus;

    private Integer totalSeats;

    private BigDecimal baseFareTotal;

    private BigDecimal discountAmount;

    private BigDecimal taxAmount;

    private BigDecimal insuranceAmount;

    private BigDecimal totalAmount;

    private String boardingPointName;

    private String droppingPointName;

    private String userId;
    
    private String userName;
    
    private String userEmail;

    private String tripId;

    private List<BookingSeatDTO> bookingSeats;

    private LocalDateTime createdAt;
}