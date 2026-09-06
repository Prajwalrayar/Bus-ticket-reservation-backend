package com.crimsonlogic.busticketbooking.ai.service.impl;

import com.crimsonlogic.busticketbooking.ai.dto.TravelOptionDTO;
import com.crimsonlogic.busticketbooking.dto.TripDTO;
import com.crimsonlogic.busticketbooking.dto.TripSearchRequest;
import com.crimsonlogic.busticketbooking.entity.Trip;
import com.crimsonlogic.busticketbooking.entity.UserPreference;
import com.crimsonlogic.busticketbooking.repository.TripRepository;
import com.crimsonlogic.busticketbooking.repository.UserPreferenceRepository;
import com.crimsonlogic.busticketbooking.ai.service.AiTravelRecommendationService;
import com.crimsonlogic.busticketbooking.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiTravelRecommendationServiceImpl implements AiTravelRecommendationService {

    private final TripRepository tripRepository;
    private final TripService tripService;
    private final UserPreferenceRepository userPreferenceRepository;

    @Value("${ai.scoring.weight.price:-1.0}")
    private Double weightPrice;

    @Value("${ai.scoring.weight.duration:-2.0}")
    private Double weightDuration;

    @Value("${ai.scoring.weight.transfers:-15.0}")
    private Double weightTransfers;

    @Value("${ai.scoring.weight.preference:10.0}")
    private Double weightPreference;

    @Override
    public List<TravelOptionDTO> getTravelOptions(String source, String destination, LocalDate travelDate, String userId) {
        List<TravelOptionDTO> options = new ArrayList<>();

        // 1. Direct Trips
        TripSearchRequest directRequest = new TripSearchRequest();
        directRequest.setSource(source);
        directRequest.setDestination(destination);
        directRequest.setTravelDate(travelDate);
        List<TripDTO> directTrips = tripService.searchTrips(directRequest);

        for (TripDTO trip : directTrips) {
            if (trip.getAvailableSeats() > 0) {
                TravelOptionDTO option = new TravelOptionDTO();
                option.setTrips(List.of(trip));
                option.setTotalPrice(trip.getBaseFare());
                option.setOptionType("DIRECT");
                
                double duration = calculateDurationHours(trip.getTravelDate(), trip.getDepartureTime(), trip.getTravelDate(), trip.getArrivalTime());
                option.setTotalDurationHours(duration);
                
                options.add(option);
            }
        }

        // 2. Connecting Trips (1 transfer)
        List<Trip> tripsFromSource = tripRepository.findByRoute_SourceIgnoreCaseAndTravelDate(source, travelDate);
        List<Trip> tripsToDest = tripRepository.findByRoute_DestinationIgnoreCaseAndTravelDateBetween(destination, travelDate, travelDate.plusDays(1));

        for (Trip trip1 : tripsFromSource) {
            // Must not be cancelled
            if (Boolean.TRUE.equals(trip1.getIsCancelled())) continue;
            
            for (Trip trip2 : tripsToDest) {
                if (Boolean.TRUE.equals(trip2.getIsCancelled())) continue;

                // Check transfer station match
                if (trip1.getRoute().getDestination().equalsIgnoreCase(trip2.getRoute().getSource())) {
                    
                    LocalDateTime arrivalTime1 = LocalDateTime.of(trip1.getTravelDate(), trip1.getArrivalTime());
                    // If arrival is before departure, but hour is e.g., 01:00, we might need to adjust travelDate.
                    // For simplicity, assuming travelDate + arrivalTime is correct. If arrivalTime < departureTime, it arrived next day.
                    if (trip1.getArrivalTime().isBefore(trip1.getDepartureTime())) {
                        arrivalTime1 = arrivalTime1.plusDays(1);
                    }

                    LocalDateTime departureTime2 = LocalDateTime.of(trip2.getTravelDate(), trip2.getDepartureTime());

                    // Minimum 30 mins transfer time
                    if (arrivalTime1.plusMinutes(30).isBefore(departureTime2) || arrivalTime1.plusMinutes(30).isEqual(departureTime2)) {
                        
                        // Maximum 12 hours waiting time
                        if (Duration.between(arrivalTime1, departureTime2).toHours() <= 12) {
                            
                            TripDTO dto1 = tripService.getTripById(trip1.getTripId());
                            TripDTO dto2 = tripService.getTripById(trip2.getTripId());
                            
                            if (dto1.getAvailableSeats() > 0 && dto2.getAvailableSeats() > 0) {
                                TravelOptionDTO option = new TravelOptionDTO();
                                option.setTrips(List.of(dto1, dto2));
                                option.setTotalPrice(dto1.getBaseFare().add(dto2.getBaseFare()));
                                option.setOptionType("CONNECTING");
                                
                                LocalDateTime arrivalTime2 = LocalDateTime.of(trip2.getTravelDate(), trip2.getArrivalTime());
                                if (trip2.getArrivalTime().isBefore(trip2.getDepartureTime())) {
                                    arrivalTime2 = arrivalTime2.plusDays(1);
                                }
                                
                                LocalDateTime departureTime1 = LocalDateTime.of(trip1.getTravelDate(), trip1.getDepartureTime());
                                double totalDuration = Duration.between(departureTime1, arrivalTime2).toMinutes() / 60.0;
                                
                                option.setTotalDurationHours(totalDuration);
                                options.add(option);
                            }
                        }
                    }
                }
            }
        }

        // 3. Score options
        UserPreference pref = null;
        if (userId != null && !userId.isBlank()) {
            Optional<UserPreference> p = userPreferenceRepository.findByUser_UserId(userId);
            if (p.isPresent()) pref = p.get();
        }

        for (TravelOptionDTO option : options) {
            double score = 100.0; // Base score
            
            // Price impact (price * weightPrice)
            score += (option.getTotalPrice().doubleValue() * weightPrice);
            
            // Duration impact (duration * weightDuration)
            score += (option.getTotalDurationHours() * weightDuration);
            
            // Transfers impact
            if (option.getTrips().size() > 1) {
                score += weightTransfers;
            }

            // Preference impact
            if (pref != null) {
                for (TripDTO trip : option.getTrips()) {
                    if (pref.getPreferredBusType() != null && trip.getBusType() != null && trip.getBusType().equalsIgnoreCase(pref.getPreferredBusType())) {
                        score += (weightPreference / option.getTrips().size());
                    }
                    if (pref.getAcPreference() != null && trip.getAmenities() != null && trip.getAmenities().contains("AC") == pref.getAcPreference()) {
                        score += (weightPreference / option.getTrips().size());
                    }
                }
            }
            
            option.setScore(score);
        }

        // Rank by score descending
        options.sort(Comparator.comparing(TravelOptionDTO::getScore).reversed());

        return options;
    }

    private double calculateDurationHours(LocalDate travelDate, java.time.LocalTime dep, LocalDate arrDate, java.time.LocalTime arr) {
        LocalDateTime start = LocalDateTime.of(travelDate, dep);
        LocalDateTime end = LocalDateTime.of(travelDate, arr);
        if (arr.isBefore(dep)) {
            end = end.plusDays(1);
        }
        return java.time.Duration.between(start, end).toMinutes() / 60.0;
    }
}
