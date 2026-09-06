package com.crimsonlogic.busticketbooking.ai.service;

import com.crimsonlogic.busticketbooking.ai.dto.SentimentAnalyticsDTO;

public interface AiSentimentService {
    
    /**
     * Analyzes all pending reviews (where sentimentStatus is null).
     * Returns the aggregate sentiment analytics.
     */
    SentimentAnalyticsDTO analyzePendingReviewsAndGetAnalytics();
}
