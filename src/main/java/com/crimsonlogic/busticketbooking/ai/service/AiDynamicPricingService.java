package com.crimsonlogic.busticketbooking.ai.service;

import com.crimsonlogic.busticketbooking.ai.dto.DynamicPricingDTO;

public interface AiDynamicPricingService {

    /**
     * Simulates dynamic pricing for a trip based on demand and scarcity.
     * @param tripId The ID of the trip to simulate pricing for.
     * @return DynamicPricingDTO containing the simulated pricing data.
     */
    DynamicPricingDTO simulatePricing(String tripId);
}
