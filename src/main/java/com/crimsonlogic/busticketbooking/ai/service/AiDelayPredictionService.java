package com.crimsonlogic.busticketbooking.ai.service;

import com.crimsonlogic.busticketbooking.ai.dto.DelayPredictionDTO;

public interface AiDelayPredictionService {
    
    /**
     * Predicts possible delays for a trip based on scheduled times, route, and simulated factors.
     * @param tripId The ID of the trip to predict delay for.
     * @return DelayPredictionDTO containing expected arrival and probability.
     */
    DelayPredictionDTO predictDelay(String tripId);
}
