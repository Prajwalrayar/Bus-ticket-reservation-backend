package com.crimsonlogic.busticketbooking.service;

import com.crimsonlogic.busticketbooking.dto.BusCreateRequest;
import com.crimsonlogic.busticketbooking.dto.BusDTO;
import com.crimsonlogic.busticketbooking.dto.BusSeatCreateRequest;
import com.crimsonlogic.busticketbooking.dto.BusSeatDTO;

import java.util.List;

public interface BusService {

    // Bus operations
    BusDTO createBus(BusCreateRequest request);

    BusDTO getBusByRegistrationNumber(String registrationNumber);

    List<BusDTO> getAllBuses();

    List<BusDTO> getBusesByOperator(String companyName);

    BusDTO updateBus(
            String registrationNumber,
            BusCreateRequest request
    );

    void deactivateBus(String registrationNumber);


    // Physical seat configuration operations
    BusSeatDTO createBusSeat(
            String busRegistrationNumber,
            BusSeatCreateRequest request
    );

    BusSeatDTO getBusSeat(
            String busRegistrationNumber,
            String seatNumber
    );

    List<BusSeatDTO> getSeatsByBus(
            String busRegistrationNumber
    );

    BusSeatDTO updateBusSeat(
            String busRegistrationNumber,
            String seatNumber,
            BusSeatCreateRequest request
    );

    void deactivateBusSeat(
            String busRegistrationNumber,
            String seatNumber
    );
}