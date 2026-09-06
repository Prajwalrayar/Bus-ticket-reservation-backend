package com.crimsonlogic.busticketbooking.dto;

import com.crimsonlogic.busticketbooking.enums.SupportTicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SupportTicketUpdateRequest {

    @NotNull(message = "Status is required")
    private SupportTicketStatus status;
}
