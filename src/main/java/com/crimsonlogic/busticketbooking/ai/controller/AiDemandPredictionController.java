package com.crimsonlogic.busticketbooking.ai.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.ai.dto.DemandPredictionDTO;
import com.crimsonlogic.busticketbooking.ai.service.AiDemandPredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/demand")
@RequiredArgsConstructor
public class AiDemandPredictionController {

    private final AiDemandPredictionService aiDemandPredictionService;

    // Typically, demand predictions should only be accessible by ADMINs or Operators
    @GetMapping("/{tripId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<DemandPredictionDTO>> getDemandPrediction(@PathVariable String tripId) {
        DemandPredictionDTO prediction = aiDemandPredictionService.predictDemand(tripId);
        return ResponseEntity.ok(ApiResponse.success(prediction));
    }
}
