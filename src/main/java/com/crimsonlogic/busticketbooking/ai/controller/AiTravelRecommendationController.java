package com.crimsonlogic.busticketbooking.ai.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.ai.dto.TravelOptionDTO;
import com.crimsonlogic.busticketbooking.ai.service.AiTravelRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ai/travel-options")
@RequiredArgsConstructor
public class AiTravelRecommendationController {

    private final AiTravelRecommendationService aiTravelRecommendationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TravelOptionDTO>>> getTravelOptions(
            @RequestParam String source,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate,
            @RequestParam(required = false) String userId) {
        
        List<TravelOptionDTO> options = aiTravelRecommendationService.getTravelOptions(source, destination, travelDate, userId);
        return ResponseEntity.ok(ApiResponse.success(options));
    }
}
