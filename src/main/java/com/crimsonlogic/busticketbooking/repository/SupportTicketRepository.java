package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, String> {

    List<SupportTicket> findByCustomer_UserId(String customerId);

    List<SupportTicket> findByOperator_OperatorId(String operatorId);
}
