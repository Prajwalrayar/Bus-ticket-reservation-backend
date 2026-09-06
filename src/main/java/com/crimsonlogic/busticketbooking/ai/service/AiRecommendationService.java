package com.crimsonlogic.busticketbooking.ai.service;

import com.crimsonlogic.busticketbooking.ai.dto.RecommendationDTO;
import java.util.List;

public interface AiRecommendationService {

    /**
     * Gets a list of smart bus recommendations for a specific user.
     * @param userId The ID of the user.
     * @return List of ranked bus recommendations with reasons.
     */
    List<RecommendationDTO> getRecommendations(String userId);
}
