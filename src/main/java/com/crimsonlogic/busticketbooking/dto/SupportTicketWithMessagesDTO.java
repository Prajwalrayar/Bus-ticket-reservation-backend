package com.crimsonlogic.busticketbooking.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SupportTicketWithMessagesDTO extends SupportTicketDTO {
    private List<SupportTicketMessageDTO> messages;
}
