package com.crimsonlogic.busticketbooking.service;

import com.crimsonlogic.busticketbooking.dto.SupportTicketCreateRequest;
import com.crimsonlogic.busticketbooking.dto.SupportTicketDTO;
import com.crimsonlogic.busticketbooking.dto.SupportTicketUpdateRequest;

import java.util.List;

public interface SupportTicketService {

    SupportTicketDTO createTicket(SupportTicketCreateRequest request);

    List<SupportTicketDTO> getMyTickets();

    List<SupportTicketDTO> getTicketsByOperator();

    SupportTicketDTO updateTicketStatus(String ticketId, SupportTicketUpdateRequest request);

    List<SupportTicketDTO> getAllTickets();

    com.crimsonlogic.busticketbooking.dto.SupportTicketWithMessagesDTO getTicketWithMessages(String ticketId);

    com.crimsonlogic.busticketbooking.dto.SupportTicketMessageDTO addMessage(String ticketId, com.crimsonlogic.busticketbooking.dto.SupportTicketReplyRequest request);

    SupportTicketDTO resolveTicket(String ticketId);
}
