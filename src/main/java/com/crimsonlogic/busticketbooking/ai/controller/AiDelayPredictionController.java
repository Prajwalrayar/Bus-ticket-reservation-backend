package com.crimsonlogic.busticketbooking.ai.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.ai.dto.DelayPredictionDTO;
import com.crimsonlogic.busticketbooking.ai.service.AiDelayPredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/delay")
@RequiredArgsConstructor
public class AiDelayPredictionController {

    private final AiDelayPredictionService aiDelayPredictionService;

    @GetMapping("/{tripId}")
    public ResponseEntity<ApiResponse<DelayPredictionDTO>> predictDelay(@PathVariable String tripId) {
        DelayPredictionDTO prediction = aiDelayPredictionService.predictDelay(tripId);
        return ResponseEntity.ok(ApiResponse.success(prediction));
    }
}
