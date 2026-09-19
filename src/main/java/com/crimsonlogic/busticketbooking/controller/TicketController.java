package com.crimsonlogic.busticketbooking.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.dto.TicketDTO;
import com.crimsonlogic.busticketbooking.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.crimsonlogic.busticketbooking.util.QRCodeGenerator;

import com.crimsonlogic.busticketbooking.service.BookingService;
import com.crimsonlogic.busticketbooking.dto.BookingDTO;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final BookingService bookingService;
    private final QRCodeGenerator qrCodeGenerator;


    // =========================================================
    // GET TICKET BY ID
    // =========================================================

    @GetMapping("/{ticketId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TicketDTO>> getTicketById(
            @PathVariable String ticketId) {

        return ResponseEntity.ok(ApiResponse.success(ticketService.getTicketById(ticketId))
        );
    }


    // =========================================================
    // GET TICKET BY TICKET NUMBER
    // =========================================================

    @GetMapping("/number/{ticketNumber}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TicketDTO>> getTicketByNumber(
            @PathVariable String ticketNumber) {

        return ResponseEntity.ok(ApiResponse.success(
                        ticketService.getTicketByNumber(ticketNumber))
        );
    }


    // =========================================================
    // GET TICKET BY BOOKING
    // =========================================================

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TicketDTO>> getTicketByBooking(
            @PathVariable String bookingId) {

        return ResponseEntity.ok(ApiResponse.success(
                        ticketService.getTicketByBooking(bookingId))
        );
    }


    // =========================================================
    // VALIDATE TICKET
    // =========================================================

    @PatchMapping("/{ticketNumber}/validate")
    @PreAuthorize("hasAnyRole('BUS_OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> validateTicket(
            @PathVariable String ticketNumber) {

        ticketService.validateTicket(ticketNumber);

        return ResponseEntity.ok(ApiResponse.success(
                        "Ticket validated successfully", null)
        );
    }

    // =========================================================
    // GET TICKET QR CODE
    // =========================================================

    @GetMapping(value = "/{ticketNumber}/qr", produces = org.springframework.http.MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> getTicketQRCode(
            @PathVariable String ticketNumber) {

        TicketDTO ticket = ticketService.getTicketByNumber(ticketNumber);
        BookingDTO booking = bookingService.getBookingById(ticket.getBookingId());

        String passengerDetails = booking.getBookingSeats().stream()
                .map(seat -> "- " + seat.getSeatNumber() + ": " + seat.getPassengerName() + " (" + seat.getPassengerAge() + " " + seat.getPassengerGender() + ")")
                .collect(Collectors.joining("\n"));
        
        String qrContent = "Ticket: " + ticket.getTicketNumber() + "\n" +
                           "Code: " + ticket.getVerificationCode() + "\n" +
                           "PNR: " + ticket.getBookingId() + "\n" +
                           "Route: " + ticket.getSource() + " -> " + ticket.getDestination() + "\n" +
                           "Date: " + ticket.getTravelDate() + " " + ticket.getDepartureTime() + "\n" +
                           "Passengers:\n" + passengerDetails;

        try {
            byte[] qrCodeImage = qrCodeGenerator.generateQRCode(qrContent, 300, 300);
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.IMAGE_PNG)
                    .body(qrCodeImage);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}