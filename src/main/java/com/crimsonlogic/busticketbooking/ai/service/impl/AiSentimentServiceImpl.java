package com.crimsonlogic.busticketbooking.ai.service.impl;

import com.crimsonlogic.busticketbooking.ai.dto.SentimentAnalyticsDTO;
import com.crimsonlogic.busticketbooking.entity.Review;
import com.crimsonlogic.busticketbooking.enums.SentimentStatus;
import com.crimsonlogic.busticketbooking.repository.ReviewRepository;
import com.crimsonlogic.busticketbooking.ai.service.AiSentimentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class AiSentimentServiceImpl implements AiSentimentService {

    private final ReviewRepository reviewRepository;
    private final ChatClient chatClient;

    public AiSentimentServiceImpl(ReviewRepository reviewRepository, ChatClient.Builder chatClientBuilder) {
        this.reviewRepository = reviewRepository;
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    @Transactional
    public SentimentAnalyticsDTO analyzePendingReviewsAndGetAnalytics() {
        List<Review> pendingReviews = reviewRepository.findBySentimentStatusIsNull();

        log.info("Found {} pending reviews for sentiment analysis", pendingReviews.size());

        for (Review review : pendingReviews) {
            analyzeSingleReview(review);
        }

        if (!pendingReviews.isEmpty()) {
            reviewRepository.saveAll(pendingReviews);
        }

        return getAnalytics();
    }

    private void analyzeSingleReview(Review review) {
        // Handle empty/invalid textual reviews safely with a heuristic based on rating
        if (review.getComment() == null || review.getComment().trim().isEmpty()) {
            applyHeuristicSentiment(review);
            return;
        }

        try {
            BeanOutputConverter<SentimentResult> converter = new BeanOutputConverter<>(SentimentResult.class);
            String format = converter.getFormat();

            String systemPrompt = String.format(
                    "You are a sentiment analysis engine for a bus ticketing platform. " +
                    "Analyze the given customer review and classify its sentiment as POSITIVE, NEUTRAL, or NEGATIVE. " +
                    "Also provide a sentiment score from -1.0 (extremely negative) to 1.0 (extremely positive). " +
                    "Ensure you provide the result in the exact JSON format requested.\n\n%%s", format);

            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(review.getComment())
                    .call()
                    .content();

            SentimentResult result = converter.convert(response);

            if (result != null) {
                review.setSentimentStatus(SentimentStatus.valueOf(result.getStatus().toUpperCase()));
                review.setSentimentScore(result.getScore());
            } else {
                applyHeuristicSentiment(review);
            }
        } catch (Exception e) {
            log.error("Failed to analyze review {}: {}", review.getReviewId(), e.getMessage());
            // Fallback to heuristic on LLM failure to ensure it is marked processed
            applyHeuristicSentiment(review);
        }
    }

    private void applyHeuristicSentiment(Review review) {
        if (review.getRating() >= 4) {
            review.setSentimentStatus(SentimentStatus.POSITIVE);
            review.setSentimentScore(0.8);
        } else if (review.getRating() == 3) {
            review.setSentimentStatus(SentimentStatus.NEUTRAL);
            review.setSentimentScore(0.0);
        } else {
            review.setSentimentStatus(SentimentStatus.NEGATIVE);
            review.setSentimentScore(-0.8);
        }
    }

    private SentimentAnalyticsDTO getAnalytics() {
        long positiveCount = reviewRepository.countBySentimentStatus(SentimentStatus.POSITIVE);
        long neutralCount = reviewRepository.countBySentimentStatus(SentimentStatus.NEUTRAL);
        long negativeCount = reviewRepository.countBySentimentStatus(SentimentStatus.NEGATIVE);
        long totalAnalyzed = positiveCount + neutralCount + negativeCount;
        
        Double avgScore = reviewRepository.getAverageSentimentScore();
        if (avgScore == null) {
            avgScore = 0.0;
        }

        return new SentimentAnalyticsDTO(totalAnalyzed, positiveCount, neutralCount, negativeCount, avgScore);
    }

    public static class SentimentResult {
        private String status;
        private Double score;

        public SentimentResult() {}

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
    }
}
