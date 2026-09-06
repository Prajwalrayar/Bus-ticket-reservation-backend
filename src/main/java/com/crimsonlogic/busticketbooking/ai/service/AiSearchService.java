package com.crimsonlogic.busticketbooking.ai.service;

import com.crimsonlogic.busticketbooking.dto.TripSearchRequest;

public interface AiSearchService {

    /**
     * Parses a natural language query and extracts structured bus search criteria.
     * @param query The natural language string (e.g. "Find AC sleeper buses from Bangalore to Chennai tonight below 1000")
     * @param userId The ID of the user performing the search (optional, for search history)
     * @return TripSearchRequest containing the structured criteria
     */
    TripSearchRequest extractSearchCriteria(String query, String userId);
}
