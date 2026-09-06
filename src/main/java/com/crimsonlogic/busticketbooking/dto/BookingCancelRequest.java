package com.crimsonlogic.busticketbooking.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingCancelRequest {

    @Size(max = 500)
    private String cancellationReason;
}