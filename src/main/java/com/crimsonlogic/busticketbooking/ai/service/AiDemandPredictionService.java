package com.crimsonlogic.busticketbooking.ai.service;

import com.crimsonlogic.busticketbooking.ai.dto.DemandPredictionDTO;

public interface AiDemandPredictionService {

    /**
     * Predicts demand for a specific trip and saves the result.
     * @param tripId The ID of the trip to predict demand for.
     * @return DemandPredictionDTO with predicted occupancy and classification.
     */
    DemandPredictionDTO predictDemand(String tripId);
}
