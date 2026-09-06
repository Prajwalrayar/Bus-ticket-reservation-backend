package com.crimsonlogic.busticketbooking.service.impl;

import com.crimsonlogic.busticketbooking.dto.SupportTicketCreateRequest;
import com.crimsonlogic.busticketbooking.dto.SupportTicketDTO;
import com.crimsonlogic.busticketbooking.dto.SupportTicketUpdateRequest;
import com.crimsonlogic.busticketbooking.entity.Operator;
import com.crimsonlogic.busticketbooking.entity.SupportTicket;
import com.crimsonlogic.busticketbooking.entity.User;
import com.crimsonlogic.busticketbooking.enums.SupportTicketStatus;
import com.crimsonlogic.busticketbooking.repository.OperatorRepository;
import com.crimsonlogic.busticketbooking.repository.SupportTicketRepository;
import com.crimsonlogic.busticketbooking.repository.UserRepository;
import com.crimsonlogic.busticketbooking.service.SupportTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SupportTicketServiceImpl implements SupportTicketService {

    private final SupportTicketRepository supportTicketRepository;
    private final UserRepository userRepository;
    private final OperatorRepository operatorRepository;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUserEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Override
    public SupportTicketDTO createTicket(SupportTicketCreateRequest request) {
        User currentUser = getAuthenticatedUser();
        
        Operator operator = operatorRepository.findById(request.getOperatorId())
                .orElseThrow(() -> new IllegalArgumentException("Operator not found"));

        SupportTicket ticket = new SupportTicket();
        ticket.setIssueSubject(request.getIssueSubject());
        ticket.setIssueDescription(request.getIssueDescription());
        ticket.setBookingReference(request.getBookingReference());
        ticket.setOperator(operator);
        ticket.setCustomer(currentUser);
        ticket.setStatus(SupportTicketStatus.OPEN);

        SupportTicket saved = supportTicketRepository.save(ticket);
        return convertToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicketDTO> getMyTickets() {
        User currentUser = getAuthenticatedUser();
        return supportTicketRepository.findByCustomer_UserId(currentUser.getUserId())
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicketDTO> getTicketsByOperator() {
        User currentUser = getAuthenticatedUser();
        if (currentUser.getOperator() == null) {
            return List.of();
        }
        return supportTicketRepository.findByOperator_OperatorId(currentUser.getOperator().getOperatorId())
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public SupportTicketDTO updateTicketStatus(String ticketId, SupportTicketUpdateRequest request) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        User currentUser = getAuthenticatedUser();
        
        boolean isAdmin = currentUser.getUserRoles().stream()
                .anyMatch(r -> r.getRoleName().equals("ADMIN"));
                
        boolean isSupportAgent = currentUser.getUserRoles().stream()
                .anyMatch(r -> r.getRoleName().equals("SUPPORT_AGENT"));

        if (!isAdmin) {
            if (!isSupportAgent || currentUser.getOperator() == null || 
                !currentUser.getOperator().getOperatorId().equals(ticket.getOperator().getOperatorId())) {
                throw new IllegalArgumentException("You are not authorized to update this ticket");
            }
        }

        ticket.setStatus(request.getStatus());
        if (isSupportAgent && ticket.getAssignedAgent() == null) {
            ticket.setAssignedAgent(currentUser);
        }

        SupportTicket saved = supportTicketRepository.save(ticket);
        return convertToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicketDTO> getAllTickets() {
        return supportTicketRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    private SupportTicketDTO convertToDTO(SupportTicket ticket) {
        SupportTicketDTO dto = new SupportTicketDTO();
        dto.setTicketId(ticket.getTicketId());
        dto.setIssueSubject(ticket.getIssueSubject());
        dto.setIssueDescription(ticket.getIssueDescription());
        dto.setStatus(ticket.getStatus());
        dto.setBookingReference(ticket.getBookingReference());
        dto.setCreatedAt(ticket.getCreatedAt());
        dto.setUpdatedAt(ticket.getUpdatedAt());
        
        if (ticket.getCustomer() != null) {
            dto.setCustomerId(ticket.getCustomer().getUserId());
            dto.setCustomerName(ticket.getCustomer().getUserName());
            dto.setCustomerEmail(ticket.getCustomer().getUserEmail());
        }
        
        if (ticket.getOperator() != null) {
            dto.setOperatorId(ticket.getOperator().getOperatorId());
            dto.setOperatorName(ticket.getOperator().getCompanyName());
        }
        
        if (ticket.getAssignedAgent() != null) {
            dto.setAssignedAgentId(ticket.getAssignedAgent().getUserId());
            dto.setAssignedAgentName(ticket.getAssignedAgent().getUserName());
        }
        
        return dto;
    }
}
