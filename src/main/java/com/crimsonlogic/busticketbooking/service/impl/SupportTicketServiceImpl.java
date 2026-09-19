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
import com.crimsonlogic.busticketbooking.repository.BookingRepository;
import com.crimsonlogic.busticketbooking.entity.Booking;
import com.crimsonlogic.busticketbooking.service.SupportTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SupportTicketServiceImpl implements SupportTicketService {

    private final SupportTicketRepository supportTicketRepository;
    private final com.crimsonlogic.busticketbooking.repository.SupportTicketMessageRepository supportTicketMessageRepository;
    private final UserRepository userRepository;
    private final OperatorRepository operatorRepository;
    private final BookingRepository bookingRepository;
    private final com.crimsonlogic.busticketbooking.service.EmailService emailService;
    private final com.crimsonlogic.busticketbooking.service.NotificationService notificationService;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUserEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private com.crimsonlogic.busticketbooking.enums.SupportTicketPriority determinePriority(String issueType) {
        if (issueType == null) return com.crimsonlogic.busticketbooking.enums.SupportTicketPriority.MEDIUM;
        
        return switch (issueType) {
            case "Amount Deducted but Booking Failed", "Bus cancelled", "Bus did not stop at boarding point" 
                -> com.crimsonlogic.busticketbooking.enums.SupportTicketPriority.HIGH;
            case "General inquiry", "Other issue" 
                -> com.crimsonlogic.busticketbooking.enums.SupportTicketPriority.LOW;
            default -> com.crimsonlogic.busticketbooking.enums.SupportTicketPriority.MEDIUM;
        };
    }

    @Override
    public SupportTicketDTO createTicket(SupportTicketCreateRequest request) {
        User currentUser = getAuthenticatedUser();
        
        Operator operator = null;
        if (request.getBookingReference() != null && !request.getBookingReference().trim().isEmpty()) {
            Booking booking = bookingRepository.findByBookingReferenceIgnoreCase(request.getBookingReference())
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found with reference: " + request.getBookingReference()));
            operator = booking.getTrip().getBus().getOperator();
        } else if (request.getOperatorId() != null && !request.getOperatorId().trim().isEmpty()) {
            operator = operatorRepository.findById(request.getOperatorId())
                    .orElseThrow(() -> new IllegalArgumentException("Operator not found"));
        }
        // operator can be null for general inquiries — ticket will be visible to all agents

        SupportTicket ticket = new SupportTicket();
        ticket.setIssueCategory(request.getIssueCategory());
        ticket.setIssueType(request.getIssueType());
        ticket.setIssueSubject(request.getIssueSubject());
        ticket.setIssueDescription(request.getIssueDescription());
        ticket.setBookingReference(request.getBookingReference());
        ticket.setAttachmentPath(request.getAttachmentPath());
        ticket.setOperator(operator);
        ticket.setCustomer(currentUser);
        ticket.setStatus(SupportTicketStatus.OPEN);
        ticket.setPriority(determinePriority(request.getIssueType()));

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
        return supportTicketRepository.findByOperator_OperatorIdOrOperatorIsNullOrderByUpdatedAtDesc(currentUser.getOperator().getOperatorId())
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
            if (!isSupportAgent || currentUser.getOperator() == null) {
                throw new IllegalArgumentException("You are not authorized to update this ticket");
            }
            if (ticket.getOperator() != null && !currentUser.getOperator().getOperatorId().equals(ticket.getOperator().getOperatorId())) {
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

    @Override
    @Transactional(readOnly = true)
    public com.crimsonlogic.busticketbooking.dto.SupportTicketWithMessagesDTO getTicketWithMessages(String ticketId) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        User currentUser = getAuthenticatedUser();
        boolean isAdmin = currentUser.getUserRoles().stream().anyMatch(r -> r.getRoleName().equals("ADMIN"));
        boolean isSupportAgent = currentUser.getUserRoles().stream().anyMatch(r -> r.getRoleName().equals("SUPPORT_AGENT"));
        boolean isCustomer = currentUser.getUserId().equals(ticket.getCustomer().getUserId());

        if (!isAdmin && !isCustomer) {
            if (!isSupportAgent || currentUser.getOperator() == null) {
                throw new IllegalArgumentException("You are not authorized to view this ticket");
            }
            if (ticket.getOperator() != null && !currentUser.getOperator().getOperatorId().equals(ticket.getOperator().getOperatorId())) {
                throw new IllegalArgumentException("You are not authorized to view this ticket");
            }
        }

        com.crimsonlogic.busticketbooking.dto.SupportTicketWithMessagesDTO dto = new com.crimsonlogic.busticketbooking.dto.SupportTicketWithMessagesDTO();
        SupportTicketDTO baseDTO = convertToDTO(ticket);
        org.springframework.beans.BeanUtils.copyProperties(baseDTO, dto);

        List<com.crimsonlogic.busticketbooking.entity.SupportTicketMessage> messages = supportTicketMessageRepository.findByTicket_TicketIdOrderByCreatedAtAsc(ticketId);
        
        List<com.crimsonlogic.busticketbooking.dto.SupportTicketMessageDTO> messageDTOs = messages.stream()
                .filter(m -> !m.isInternal() || isAdmin || isSupportAgent) // Hide internal from customers
                .map(this::convertMessageToDTO)
                .toList();

        dto.setMessages(messageDTOs);
        return dto;
    }

    @Override
    public com.crimsonlogic.busticketbooking.dto.SupportTicketMessageDTO addMessage(String ticketId, com.crimsonlogic.busticketbooking.dto.SupportTicketReplyRequest request) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        
        User currentUser = getAuthenticatedUser();
        boolean isSupportAgent = currentUser.getUserRoles().stream().anyMatch(r -> r.getRoleName().equals("SUPPORT_AGENT"));
        boolean isAdmin = currentUser.getUserRoles().stream().anyMatch(r -> r.getRoleName().equals("ADMIN"));
        boolean isCustomer = currentUser.getUserId().equals(ticket.getCustomer().getUserId());

        if (!isAdmin && !isCustomer) {
            if (!isSupportAgent || currentUser.getOperator() == null) {
                throw new IllegalArgumentException("You are not authorized to reply to this ticket");
            }
            if (ticket.getOperator() != null && !currentUser.getOperator().getOperatorId().equals(ticket.getOperator().getOperatorId())) {
                throw new IllegalArgumentException("You are not authorized to reply to this ticket");
            }
        }

        com.crimsonlogic.busticketbooking.entity.SupportTicketMessage message = new com.crimsonlogic.busticketbooking.entity.SupportTicketMessage();
        message.setTicket(ticket);
        message.setSender(currentUser);
        message.setMessage(request.getMessage());
        message.setInternal(request.isInternal() && (isSupportAgent || isAdmin));

        supportTicketMessageRepository.save(message);

        if (!message.isInternal()) {
            if (isCustomer) {
                ticket.setStatus(SupportTicketStatus.PASSENGER_REPLIED);
            } else if (isSupportAgent || isAdmin) {
                ticket.setStatus(SupportTicketStatus.WAITING_FOR_PASSENGER);
                if (ticket.getAssignedAgent() == null && isSupportAgent) {
                    ticket.setAssignedAgent(currentUser);
                }
                // Notify customer about the new reply via Email
                try {
                    emailService.sendTicketReplyEmail(
                            ticket.getCustomer().getUserEmail(),
                            ticket.getTicketId(),
                            ticket.getCustomer().getUserName()
                    );
                } catch (Exception e) {
                    log.error("Failed to send reply email for ticket {}", ticket.getTicketId(), e);
                }

                // Notify customer about the new reply via In-App Notification
                try {
                    notificationService.sendSupportTicketNotification(
                            ticket,
                            "New Reply on Support Ticket",
                            "An agent has replied to your ticket #" + ticket.getTicketId()
                    );
                } catch (Exception e) {
                    log.error("Failed to send in-app notification for ticket {}", ticket.getTicketId(), e);
                }
            }
            supportTicketRepository.save(ticket);
        }

        return convertMessageToDTO(message);
    }

    @Override
    public SupportTicketDTO resolveTicket(String ticketId) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        
        User currentUser = getAuthenticatedUser();
        boolean isAdmin = currentUser.getUserRoles().stream().anyMatch(r -> r.getRoleName().equals("ADMIN"));
        boolean isSupportAgent = currentUser.getUserRoles().stream().anyMatch(r -> r.getRoleName().equals("SUPPORT_AGENT"));
        boolean isCustomer = currentUser.getUserId().equals(ticket.getCustomer().getUserId());

        if (!isAdmin && !isCustomer) {
            if (!isSupportAgent || currentUser.getOperator() == null) {
                throw new IllegalArgumentException("You are not authorized to resolve this ticket");
            }
            if (ticket.getOperator() != null && !currentUser.getOperator().getOperatorId().equals(ticket.getOperator().getOperatorId())) {
                throw new IllegalArgumentException("You are not authorized to resolve this ticket");
            }
        }

        ticket.setStatus(SupportTicketStatus.RESOLVED);
        ticket.setResolvedAt(java.time.LocalDateTime.now());
        supportTicketRepository.save(ticket);
        
        try {
            emailService.sendTicketResolvedEmail(
                    ticket.getCustomer().getUserEmail(),
                    ticket.getTicketId(),
                    ticket.getCustomer().getUserName()
            );
        } catch (Exception e) {
            log.error("Failed to send resolved email for ticket {}", ticket.getTicketId(), e);
        }

        try {
            notificationService.sendSupportTicketNotification(
                    ticket,
                    "Support Ticket Resolved",
                    "Your ticket #" + ticket.getTicketId() + " has been marked as resolved."
            );
        } catch (Exception e) {
            log.error("Failed to send in-app notification for ticket {}", ticket.getTicketId(), e);
        }

        return convertToDTO(ticket);
    }

    private com.crimsonlogic.busticketbooking.dto.SupportTicketMessageDTO convertMessageToDTO(com.crimsonlogic.busticketbooking.entity.SupportTicketMessage msg) {
        com.crimsonlogic.busticketbooking.dto.SupportTicketMessageDTO dto = new com.crimsonlogic.busticketbooking.dto.SupportTicketMessageDTO();
        dto.setMessageId(msg.getMessageId());
        dto.setMessage(msg.getMessage());
        dto.setInternal(msg.isInternal());
        dto.setCreatedAt(msg.getCreatedAt());
        dto.setSenderId(msg.getSender().getUserId());
        dto.setSenderName(msg.getSender().getUserName());
        dto.setSenderRole(msg.getSender().getUserRoles().stream().map(r -> r.getRoleName()).findFirst().orElse("CUSTOMER"));
        return dto;
    }

    private SupportTicketDTO convertToDTO(SupportTicket ticket) {
        SupportTicketDTO dto = new SupportTicketDTO();
        dto.setTicketId(ticket.getTicketId());
        dto.setIssueCategory(ticket.getIssueCategory());
        dto.setIssueType(ticket.getIssueType());
        dto.setIssueSubject(ticket.getIssueSubject());
        dto.setIssueDescription(ticket.getIssueDescription());
        dto.setAttachmentPath(ticket.getAttachmentPath());
        dto.setStatus(ticket.getStatus());
        dto.setPriority(ticket.getPriority());
        dto.setResolvedAt(ticket.getResolvedAt());
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
