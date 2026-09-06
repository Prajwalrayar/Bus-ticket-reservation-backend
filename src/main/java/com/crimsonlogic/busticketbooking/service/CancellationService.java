package com.crimsonlogic.busticketbooking.service;

import com.crimsonlogic.busticketbooking.dto.CancellationDTO;
import com.crimsonlogic.busticketbooking.dto.CancellationRequest;


public interface CancellationService {

    CancellationDTO cancelBooking(
            String bookingId,
            CancellationRequest request
    );

    CancellationDTO getCancellationById(
            String cancellationId
    );

    CancellationDTO getCancellationByBooking(
            String bookingId
    );

    CancellationDTO getCancellationByReference(
            String cancellationReference
    );
}