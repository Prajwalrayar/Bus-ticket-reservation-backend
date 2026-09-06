package com.crimsonlogic.busticketbooking.ai.service.impl;

import com.crimsonlogic.busticketbooking.ai.dto.DemandPredictionDTO;
import com.crimsonlogic.busticketbooking.entity.DemandPrediction;
import com.crimsonlogic.busticketbooking.entity.Trip;
import com.crimsonlogic.busticketbooking.enums.BookingStatus;
import com.crimsonlogic.busticketbooking.repository.BookingRepository;
import com.crimsonlogic.busticketbooking.repository.DemandPredictionRepository;
import com.crimsonlogic.busticketbooking.repository.TripRepository;
import com.crimsonlogic.busticketbooking.ai.service.AiDemandPredictionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class AiDemandPredictionServiceImpl implements AiDemandPredictionService {

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final DemandPredictionRepository demandPredictionRepository;

    public AiDemandPredictionServiceImpl(TripRepository tripRepository,
                                         BookingRepository bookingRepository,
                                         DemandPredictionRepository demandPredictionRepository) {
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
        this.demandPredictionRepository = demandPredictionRepository;
    }

    @Override
    @Transactional
    public DemandPredictionDTO predictDemand(String tripId) {
        
        Optional<DemandPrediction> existing = demandPredictionRepository.findByTrip_TripId(tripId);
        if (existing.isPresent()) {
            return convertToDTO(existing.get());
        }

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found"));

        long totalHistoricalBookings = bookingRepository.countByTrip_Route_RouteIdAndBookingStatus(
                trip.getRoute().getRouteId(), BookingStatus.CONFIRMED);

        String dayOfWeek = trip.getTravelDate().getDayOfWeek().name();
        boolean isWeekend = dayOfWeek.equals("SATURDAY") || dayOfWeek.equals("SUNDAY");
        boolean isNearHoliday = trip.getTravelDate().getMonthValue() == 12 && trip.getTravelDate().getDayOfMonth() > 20;
        
        long daysUntilDeparture = ChronoUnit.DAYS.between(LocalDate.now(), trip.getTravelDate());
        if (daysUntilDeparture < 0) {
            daysUntilDeparture = 0; // Trip is in the past or today
        }

        // 1. Base occupancy derived from historical popularity
        double baseOccupancy = 0.5; // Default medium
        if (totalHistoricalBookings > 50) {
            baseOccupancy = 0.8;
        } else if (totalHistoricalBookings < 20) {
            baseOccupancy = 0.3;
        }

        // 2. Adjust based on external factors
        if (isNearHoliday) {
            baseOccupancy += 0.2;
        } else if (isWeekend) {
            baseOccupancy += 0.1;
        }
        
        if (daysUntilDeparture <= 3) {
            // Last minute surge
            baseOccupancy += 0.15;
        } else if (daysUntilDeparture > 30) {
            // Far in advance, less urgency
            baseOccupancy -= 0.1;
        }

        // 3. Ensure bounds between 0.0 and 1.0
        double predictedOccupancy = Math.max(0.0, Math.min(1.0, baseOccupancy));

        // 4. Classify
        String classification;
        if (predictedOccupancy > 0.70) {
            classification = "HIGH";
        } else if (predictedOccupancy < 0.40) {
            classification = "LOW";
        } else {
            classification = "MEDIUM";
        }

        DemandPredictionDTO predictionDto = new DemandPredictionDTO();
        predictionDto.setTripId(tripId);
        predictionDto.setPredictedOccupancy(predictedOccupancy);
        predictionDto.setDemandClassification(classification);
        predictionDto.setPredictedAt(java.time.LocalDateTime.now());

        DemandPrediction prediction = new DemandPrediction();
        prediction.setTrip(trip);
        prediction.setPredictedOccupancy(predictionDto.getPredictedOccupancy());
        prediction.setDemandClassification(predictionDto.getDemandClassification());
        demandPredictionRepository.save(prediction);

        return predictionDto;
    }

    private DemandPredictionDTO convertToDTO(DemandPrediction prediction) {
        return new DemandPredictionDTO(
                prediction.getTrip().getTripId(),
                prediction.getPredictedOccupancy(),
                prediction.getDemandClassification(),
                prediction.getPredictedAt()
        );
    }
}
