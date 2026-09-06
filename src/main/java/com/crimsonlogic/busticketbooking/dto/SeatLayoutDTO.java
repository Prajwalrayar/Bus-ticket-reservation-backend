package com.crimsonlogic.busticketbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Groups trip seats into Lower and Upper deck for the 2D seat map.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatLayoutDTO {

    private String tripId;
    private String operatorName;
    private String busType;
    private String source;
    private String destination;

    /** Seats on the lower deck, ordered by seat number. */
    private List<TripSeatDTO> lowerDeck;

    /** Seats on the upper deck, ordered by seat number. */
    private List<TripSeatDTO> upperDeck;

    private int totalSeats;
    private int availableSeats;
}
