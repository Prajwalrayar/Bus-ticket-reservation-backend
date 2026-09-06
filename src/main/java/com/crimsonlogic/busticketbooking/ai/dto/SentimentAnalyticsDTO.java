package com.crimsonlogic.busticketbooking.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SentimentAnalyticsDTO {
    private long totalAnalyzed;
    private long totalPositive;
    private long totalNeutral;
    private long totalNegative;
    private Double averageSentimentScore;
}
