package com.crimsonlogic.busticketbooking.service;

import com.crimsonlogic.busticketbooking.dto.SavedPassengerDTO;
import com.crimsonlogic.busticketbooking.dto.SavedPassengerRequest;

import java.util.List;

public interface SavedPassengerService {

    List<SavedPassengerDTO> getMySavedPassengers();

    SavedPassengerDTO getMySavedPassenger(
            String savedPassengerId
    );

    SavedPassengerDTO createSavedPassenger(
            SavedPassengerRequest request
    );

    SavedPassengerDTO updateSavedPassenger(
            String savedPassengerId,
            SavedPassengerRequest request
    );

    void deactivateSavedPassenger(
            String savedPassengerId
    );
}