package com.crimsonlogic.busticketbooking.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class SupportTicketMessageDTO {
    private String messageId;
    private String senderId;
    private String senderName;
    private String senderRole;
    private String message;
    private boolean isInternal;
    private LocalDateTime createdAt;
}
