package com.crimsonlogic.busticketbooking.ai.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.ai.dto.SentimentAnalyticsDTO;
import com.crimsonlogic.busticketbooking.ai.service.AiSentimentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/review")
@RequiredArgsConstructor
public class AiSentimentController {

    private final AiSentimentService aiSentimentService;

    @PostMapping("/sentiment")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<SentimentAnalyticsDTO>> analyzeReviews() {
        SentimentAnalyticsDTO analytics = aiSentimentService.analyzePendingReviewsAndGetAnalytics();
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }
}
