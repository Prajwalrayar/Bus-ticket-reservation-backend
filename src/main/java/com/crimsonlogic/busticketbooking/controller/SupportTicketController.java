package com.crimsonlogic.busticketbooking.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.dto.SupportTicketCreateRequest;
import com.crimsonlogic.busticketbooking.dto.SupportTicketDTO;
import com.crimsonlogic.busticketbooking.dto.SupportTicketUpdateRequest;
import com.crimsonlogic.busticketbooking.service.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support-tickets")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SupportTicketDTO>> createTicket(
            @Valid @RequestBody SupportTicketCreateRequest request) {
        SupportTicketDTO ticket = supportTicketService.createTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Support ticket created successfully", ticket));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SupportTicketDTO>>> getMyTickets() {
        return ResponseEntity.ok(ApiResponse.success(supportTicketService.getMyTickets()));
    }

    @GetMapping("/operator")
    @PreAuthorize("hasAnyRole('SUPPORT_AGENT', 'BUS_OPERATOR')")
    public ResponseEntity<ApiResponse<List<SupportTicketDTO>>> getTicketsByOperator() {
        return ResponseEntity.ok(ApiResponse.success(supportTicketService.getTicketsByOperator()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SupportTicketDTO>>> getAllTickets() {
        return ResponseEntity.ok(ApiResponse.success(supportTicketService.getAllTickets()));
    }

    @PatchMapping("/{ticketId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ResponseEntity<ApiResponse<SupportTicketDTO>> updateTicketStatus(
            @PathVariable String ticketId,
            @Valid @RequestBody SupportTicketUpdateRequest request) {
        SupportTicketDTO ticket = supportTicketService.updateTicketStatus(ticketId, request);
        return ResponseEntity.ok(ApiResponse.success("Ticket status updated", ticket));
    }
}
