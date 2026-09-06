package com.crimsonlogic.busticketbooking.service;

import com.crimsonlogic.busticketbooking.dto.TicketDTO;

public interface TicketService {

    TicketDTO getTicketById(String ticketId);

    TicketDTO getTicketByNumber(String ticketNumber);

    TicketDTO getTicketByBooking(String bookingId);

    void validateTicket(String ticketNumber);
}
