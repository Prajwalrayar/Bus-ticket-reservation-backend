package com.crimsonlogic.busticketbooking.ai.service.impl;

import com.crimsonlogic.busticketbooking.ai.dto.DemandPredictionDTO;
import com.crimsonlogic.busticketbooking.ai.dto.DynamicPricingDTO;
import com.crimsonlogic.busticketbooking.entity.Trip;
import com.crimsonlogic.busticketbooking.repository.TripRepository;
import com.crimsonlogic.busticketbooking.ai.service.AiDemandPredictionService;
import com.crimsonlogic.busticketbooking.ai.service.AiDynamicPricingService;
import com.crimsonlogic.busticketbooking.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class AiDynamicPricingServiceImpl implements AiDynamicPricingService {

    private final AiDemandPredictionService aiDemandPredictionService;
    private final TripService tripService;
    private final TripRepository tripRepository;

    @Value("${ai.pricing.multiplier.medium:1.15}")
    private BigDecimal multiplierMedium;

    @Value("${ai.pricing.multiplier.high:1.35}")
    private BigDecimal multiplierHigh;

    @Value("${ai.pricing.threshold.scarcity:5}")
    private int thresholdScarcity;

    @Value("${ai.pricing.multiplier.scarcity:1.1}")
    private BigDecimal multiplierScarcity;

    @Override
    public DynamicPricingDTO simulatePricing(String tripId) {
        
        // 1. Fetch trip and its base fare
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found"));
        BigDecimal baseFare = trip.getBaseFare();
        
        // 2. Fetch Demand Prediction (this will calculate if not cached)
        DemandPredictionDTO prediction = aiDemandPredictionService.predictDemand(tripId);
        
        // 3. Fetch Available Seats
        int availableSeats = tripService.getTripById(tripId).getAvailableSeats();
        
        // 4. Calculate Simulated Fare
        BigDecimal simulatedFare = baseFare;
        StringBuilder reasoning = new StringBuilder("Base fare: ").append(baseFare);

        if ("HIGH".equalsIgnoreCase(prediction.getDemandClassification())) {
            simulatedFare = simulatedFare.multiply(multiplierHigh);
            reasoning.append(" | High Demand multiplier applied (x").append(multiplierHigh).append(")");
        } else if ("MEDIUM".equalsIgnoreCase(prediction.getDemandClassification())) {
            simulatedFare = simulatedFare.multiply(multiplierMedium);
            reasoning.append(" | Medium Demand multiplier applied (x").append(multiplierMedium).append(")");
        } else {
            reasoning.append(" | Low Demand (Normal Fare)");
        }

        boolean scarcityApplied = false;
        if (availableSeats > 0 && availableSeats <= thresholdScarcity) {
            simulatedFare = simulatedFare.multiply(multiplierScarcity);
            scarcityApplied = true;
            reasoning.append(" | Scarcity multiplier applied (x").append(multiplierScarcity).append("), only ")
                     .append(availableSeats).append(" seats left.");
        }

        // 5. Build DTO
        DynamicPricingDTO dto = new DynamicPricingDTO();
        dto.setTripId(tripId);
        dto.setBaseFare(baseFare);
        dto.setSimulatedFare(simulatedFare.setScale(2, RoundingMode.HALF_UP));
        dto.setDemandClassification(prediction.getDemandClassification());
        dto.setAvailableSeats(availableSeats);
        dto.setScarcityApplied(scarcityApplied);
        dto.setReasoning(reasoning.toString());

        return dto;
    }
}
