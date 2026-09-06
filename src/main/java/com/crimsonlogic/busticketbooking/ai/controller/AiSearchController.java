package com.crimsonlogic.busticketbooking.ai.controller;

import com.crimsonlogic.busticketbooking.ai.dto.AiSearchRequest;
import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.dto.TripDTO;
import com.crimsonlogic.busticketbooking.dto.TripSearchRequest;
import com.crimsonlogic.busticketbooking.ai.service.AiSearchService;
import com.crimsonlogic.busticketbooking.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiSearchController {

    private final AiSearchService aiSearchService;
    private final TripService tripService;

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<List<TripDTO>>> naturalLanguageSearch(
            @Valid @RequestBody AiSearchRequest aiSearchRequest) {

        String userId = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            // Ideally, userDetails implementation holds the userId, or it's fetched via username (email)
            // Assuming username is the email, and we can fetch the user if needed. 
            // In many JWT setups, the username is extracted directly. We'll leave it simple.
            // If the system's UserDetailsService sets the username to email, we might need a fetch, 
            // but for SearchHistory, we can use a simpler approach or skip it if userId is not directly available.
            // For now, let's keep it null unless easily obtainable.
        }

        // 1. Convert Natural Language into Structured Criteria
        TripSearchRequest criteria = aiSearchService.extractSearchCriteria(aiSearchRequest.getQuery(), userId);

        // 2. Delegate to existing search functionality
        List<TripDTO> results = tripService.searchTrips(criteria);

        return ResponseEntity.ok(ApiResponse.success(results));
    }
}
