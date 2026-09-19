package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.SupportTicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketMessageRepository extends JpaRepository<SupportTicketMessage, String> {
    List<SupportTicketMessage> findByTicket_TicketIdOrderByCreatedAtAsc(String ticketId);
}
