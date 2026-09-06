package com.crimsonlogic.busticketbooking.dto;

import com.crimsonlogic.busticketbooking.enums.SupportTicketStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class SupportTicketDTO {
    private String ticketId;
    private String issueSubject;
    private String issueDescription;
    private SupportTicketStatus status;
    private String bookingReference;
    private String customerId;
    private String customerName;
    private String customerEmail;
    private String operatorId;
    private String operatorName;
    private String assignedAgentId;
    private String assignedAgentName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
