package com.crimsonlogic.busticketbooking.service.impl;

import com.crimsonlogic.busticketbooking.dto.TicketDTO;
import com.crimsonlogic.busticketbooking.entity.Booking;
import com.crimsonlogic.busticketbooking.entity.BookingSeat;
import com.crimsonlogic.busticketbooking.entity.Ticket;
import com.crimsonlogic.busticketbooking.entity.User;
import com.crimsonlogic.busticketbooking.repository.TicketRepository;
import com.crimsonlogic.busticketbooking.repository.UserRepository;
import com.crimsonlogic.busticketbooking.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;


    // =========================================================
    // GET TICKET BY ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public TicketDTO getTicketById(
            String ticketId) {

        Ticket ticket =
                ticketRepository.findById(ticketId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Ticket not found"
                                )
                        );

        return convertToDTO(ticket);
    }


    // =========================================================
    // GET TICKET BY TICKET NUMBER
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public TicketDTO getTicketByNumber(
            String ticketNumber) {

        Ticket ticket =
                ticketRepository
                        .findByTicketNumberIgnoreCase(
                                ticketNumber
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Ticket with number '"
                                                + ticketNumber
                                                + "' not found"
                                )
                        );

        return convertToDTO(ticket);
    }


    // =========================================================
    // GET TICKET BY BOOKING
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public TicketDTO getTicketByBooking(
            String bookingId) {

        Ticket ticket =
                ticketRepository
                        .findByBooking_BookingId(
                                bookingId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Ticket for booking '"
                                                + bookingId
                                                + "' not found"
                                )
                        );

        return convertToDTO(ticket);
    }


    // =========================================================
    // VALIDATE TICKET
    // =========================================================

    @Override
    public void validateTicket(
            String ticketNumber) {

        Ticket ticket =
                ticketRepository
                        .findByTicketNumberIgnoreCase(
                                ticketNumber
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Ticket with number '"
                                                + ticketNumber
                                                + "' not found"
                                )
                        );


        /*
         * A ticket can only be validated once.
         */
        if (ticket.getValidatedAt() != null) {

            throw new IllegalArgumentException(
                    "Ticket has already been validated"
            );
        }


        /*
         * Find the staff/admin user who is
         * performing the validation from Spring Security auth context.
         */
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User validatedByUser =
                userRepository.findByUserEmail(email)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Validating user not found"
                                )
                        );


        /*
         * Record validation information.
         */
        ticket.setValidatedByUser(
                validatedByUser
        );

        ticket.setValidatedAt(
                LocalDateTime.now()
        );

        ticketRepository.save(ticket);
    }


    // =========================================================
    // ENTITY â†’ DTO
    // =========================================================

    private TicketDTO convertToDTO(
            Ticket ticket) {

        TicketDTO dto =
                new TicketDTO();

        dto.setTicketId(
                ticket.getTicketId()
        );

        dto.setTicketNumber(
                ticket.getTicketNumber()
        );

        dto.setVerificationCode(
                ticket.getVerificationCode()
        );

        dto.setIssuedAt(
                ticket.getIssuedAt()
        );

        dto.setValidatedAt(
                ticket.getValidatedAt()
        );


        Booking booking =
                ticket.getBooking();

        if (booking == null) {
            return dto;
        }


        dto.setBookingId(
                booking.getBookingId()
        );

        dto.setTotalSeats(
                booking.getTotalSeats()
        );

        dto.setTotalAmount(
                booking.getTotalAmount()
        );


        // -----------------------------------------------------
        // Trip information
        // -----------------------------------------------------

        if (booking.getTrip() != null) {

            var trip =
                    booking.getTrip();

            dto.setTravelDate(
                    trip.getTravelDate()
            );

            dto.setDepartureTime(
                    trip.getDepartureTime()
            );

            dto.setArrivalTime(
                    trip.getArrivalTime()
            );


            // -------------------------------------------------
            // Bus information
            // -------------------------------------------------

            if (trip.getBus() != null) {

                var bus =
                        trip.getBus();

                dto.setBusNumber(
                        bus.getRegistrationNumber()
                );

                dto.setBusType(
                        bus.getBusType()
                );

                if (bus.getOperator() != null) {

                    dto.setOperatorName(
                            bus.getOperator()
                                    .getCompanyName()
                    );
                }
            }


            // -------------------------------------------------
            // Route information
            // -------------------------------------------------

            if (trip.getRoute() != null) {

                dto.setSource(
                        trip.getRoute()
                                .getSource()
                );

                dto.setDestination(
                        trip.getRoute()
                                .getDestination()
                );
            }
        }


        // -----------------------------------------------------
        // Boarding / dropping points
        // -----------------------------------------------------

        if (booking.getBoardingPoint() != null) {

            dto.setBoardingPoint(
                    booking.getBoardingPoint()
                            .getStopName()
            );
        }

        if (booking.getDroppingPoint() != null) {

            dto.setDroppingPoint(
                    booking.getDroppingPoint()
                            .getStopName()
            );
        }


        // -----------------------------------------------------
        // Booked seat numbers
        // -----------------------------------------------------

        if (booking.getBookingSeats() != null) {

            List<String> seatNumbers =
                    booking.getBookingSeats()
                            .stream()
                            .map(BookingSeat::getTripSeat)
                            .filter(java.util.Objects::nonNull)
                            .map(tripSeat ->
                                    tripSeat.getBusSeat()
                                            .getSeatNumber()
                            )
                            .toList();

            dto.setSeatNumbers(
                    seatNumbers
            );
        }

        return dto;
    }
}