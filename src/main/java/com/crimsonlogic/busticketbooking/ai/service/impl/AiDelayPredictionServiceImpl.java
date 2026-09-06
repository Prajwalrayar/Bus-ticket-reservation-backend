package com.crimsonlogic.busticketbooking.ai.service.impl;

import com.crimsonlogic.busticketbooking.ai.dto.DelayPredictionDTO;
import com.crimsonlogic.busticketbooking.entity.Trip;
import com.crimsonlogic.busticketbooking.repository.TripRepository;
import com.crimsonlogic.busticketbooking.ai.service.AiDelayPredictionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Slf4j
@Service
public class AiDelayPredictionServiceImpl implements AiDelayPredictionService {

    private final TripRepository tripRepository;
    private final ChatClient chatClient;

    public AiDelayPredictionServiceImpl(TripRepository tripRepository, ChatClient.Builder chatClientBuilder) {
        this.tripRepository = tripRepository;
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public DelayPredictionDTO predictDelay(String tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found"));

        String dayOfWeek = trip.getTravelDate().getDayOfWeek().name();
        String source = trip.getRoute().getSource();
        String destination = trip.getRoute().getDestination();
        String distance = trip.getRoute().getDistance().toString();
        String departureTime = trip.getDepartureTime().toString();

        BeanOutputConverter<DelayResult> converter = new BeanOutputConverter<>(DelayResult.class);
        String format = converter.getFormat();

        String systemPrompt = String.format(
                "You are an AI delay prediction engine for a bus system. " +
                "Since real-time GPS APIs are offline, you must SIMULATE a realistic delay prediction based on these parameters: " +
                "Source: %s, Destination: %s, Distance: %s km, Departure Time: %s, Day of Week: %s. " +
                "Consider typical simulated traffic/weather for this context (e.g. Friday evenings have higher delay probability). " +
                "Predict the delay in minutes (can be 0 if on time) and a probability (0.0 to 1.0). " +
                "Return exactly the requested JSON format.\n\n%%s",
                source, destination, distance, departureTime, dayOfWeek, format);

        String response = chatClient.prompt()
                .system(systemPrompt)
                .user("Predict delay for trip: " + tripId)
                .call()
                .content();

        DelayResult llmResult;
        try {
            llmResult = converter.convert(response);
        } catch (Exception e) {
            log.error("Failed to parse LLM delay prediction: {}", response, e);
            // Safe fallback if parsing fails
            llmResult = new DelayResult();
            llmResult.setDelayMinutes(0);
            llmResult.setDelayProbability(0.0);
            llmResult.setReasoning("Prediction engine unavailable. Assuming on-time arrival.");
        }

        LocalTime expectedArrival = trip.getArrivalTime().plusMinutes(llmResult.getDelayMinutes() != null ? llmResult.getDelayMinutes() : 0);

        DelayPredictionDTO dto = new DelayPredictionDTO();
        dto.setTripId(tripId);
        dto.setScheduledArrivalTime(trip.getArrivalTime());
        dto.setExpectedArrivalTime(expectedArrival);
        dto.setDelayProbability(llmResult.getDelayProbability() != null ? llmResult.getDelayProbability() : 0.0);
        dto.setReasoning(llmResult.getReasoning());

        return dto;
    }

    public static class DelayResult {
        private Integer delayMinutes;
        private Double delayProbability;
        private String reasoning;

        public DelayResult() {}

        public Integer getDelayMinutes() { return delayMinutes; }
        public void setDelayMinutes(Integer delayMinutes) { this.delayMinutes = delayMinutes; }
        public Double getDelayProbability() { return delayProbability; }
        public void setDelayProbability(Double delayProbability) { this.delayProbability = delayProbability; }
        public String getReasoning() { return reasoning; }
        public void setReasoning(String reasoning) { this.reasoning = reasoning; }
    }
}
