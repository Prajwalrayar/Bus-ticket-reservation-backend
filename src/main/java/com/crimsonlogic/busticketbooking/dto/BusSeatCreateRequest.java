package com.crimsonlogic.busticketbooking.dto;

import com.crimsonlogic.busticketbooking.enums.SeatPosition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusSeatCreateRequest {

    @NotBlank
    private String seatNumber;

    @NotNull
    private SeatPosition seatPosition;
}