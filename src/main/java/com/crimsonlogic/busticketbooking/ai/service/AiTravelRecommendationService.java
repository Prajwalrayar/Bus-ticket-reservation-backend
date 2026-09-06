package com.crimsonlogic.busticketbooking.ai.service;

import com.crimsonlogic.busticketbooking.ai.dto.TravelOptionDTO;

import java.time.LocalDate;
import java.util.List;

public interface AiTravelRecommendationService {
    List<TravelOptionDTO> getTravelOptions(String source, String destination, LocalDate travelDate, String userId);
}
