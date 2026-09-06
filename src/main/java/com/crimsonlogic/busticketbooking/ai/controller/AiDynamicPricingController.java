package com.crimsonlogic.busticketbooking.ai.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.ai.dto.DynamicPricingDTO;
import com.crimsonlogic.busticketbooking.ai.service.AiDynamicPricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/price")
@RequiredArgsConstructor
public class AiDynamicPricingController {

    private final AiDynamicPricingService aiDynamicPricingService;

    // Simulation endpoint can be open or restricted. We restrict it to Operator/Admin for safety.
    @GetMapping("/{tripId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<DynamicPricingDTO>> simulatePrice(@PathVariable String tripId) {
        DynamicPricingDTO simulation = aiDynamicPricingService.simulatePricing(tripId);
        return ResponseEntity.ok(ApiResponse.success(simulation));
    }
}
