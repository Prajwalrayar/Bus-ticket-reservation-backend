package com.crimsonlogic.busticketbooking.ai.service;

import com.crimsonlogic.busticketbooking.ai.dto.PersonalizedOfferDTO;

import java.util.List;

public interface AiOfferGenerationService {
    
    /**
     * Generates a new personalized offer for the user if they don't have an active one,
     * and returns the list of all their offers.
     */
    List<PersonalizedOfferDTO> getOrGenerateOffersForUser(String userId);

}
