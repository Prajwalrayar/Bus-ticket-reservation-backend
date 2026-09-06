package com.crimsonlogic.busticketbooking.ai.service.impl;

import com.crimsonlogic.busticketbooking.ai.dto.RecommendationDTO;
import com.crimsonlogic.busticketbooking.dto.TripDTO;
import com.crimsonlogic.busticketbooking.dto.TripSearchRequest;
import com.crimsonlogic.busticketbooking.entity.Booking;
import com.crimsonlogic.busticketbooking.entity.SearchHistory;
import com.crimsonlogic.busticketbooking.entity.UserPreference;
import com.crimsonlogic.busticketbooking.repository.BookingRepository;
import com.crimsonlogic.busticketbooking.repository.SearchHistoryRepository;
import com.crimsonlogic.busticketbooking.repository.UserPreferenceRepository;
import com.crimsonlogic.busticketbooking.ai.service.AiRecommendationService;
import com.crimsonlogic.busticketbooking.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiRecommendationServiceImpl implements AiRecommendationService {

    private final TripService tripService;
    private final UserPreferenceRepository userPreferenceRepository;
    private final BookingRepository bookingRepository;
    private final SearchHistoryRepository searchHistoryRepository;

    @Override
    public List<RecommendationDTO> getRecommendations(String userId) {

        // 1. Determine most relevant route (source to destination)
        String[] route = determineRelevantRoute(userId);
        if (route == null) {
            // Cannot recommend without a route context. Return empty or fallback.
            return new ArrayList<>();
        }
        String source = route[0];
        String destination = route[1];

        // 2. Fetch all upcoming trips for this route using existing search logic
        TripSearchRequest searchRequest = new TripSearchRequest();
        searchRequest.setSource(source);
        searchRequest.setDestination(destination);
        // Leaving travelDate null fetches all dates, but we want only upcoming. 
        // We will fetch all and filter manually, or set travelDate to today.
        // The service filters by departureStart if provided.
        searchRequest.setDepartureStart(java.time.LocalTime.MIN); // Just ensuring it's not null if we want strict time, actually null is better.

        List<TripDTO> availableTrips = tripService.searchTrips(searchRequest);

        // Filter out past trips and fully booked trips
        availableTrips = availableTrips.stream()
                .filter(trip -> trip.getTravelDate().isAfter(LocalDate.now().minusDays(1)))
                .filter(trip -> trip.getAvailableSeats() > 0)
                .toList();

        if (availableTrips.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. Score trips based on UserPreference
        UserPreference preference = userPreferenceRepository.findByUser_UserId(userId).orElse(null);

        List<RecommendationDTO> recommendations = availableTrips.stream()
                .map(trip -> scoreTrip(trip, preference))
                .sorted(Comparator.comparing(RecommendationDTO::getScore).reversed())
                .limit(5)
                .toList();

        return recommendations;
    }

    private String[] determineRelevantRoute(String userId) {
        // Try recent search history first
        List<SearchHistory> searches = searchHistoryRepository.findByUser_UserId(userId);
        if (searches != null && !searches.isEmpty()) {
            searches.sort((s1, s2) -> s2.getSearchedAt().compareTo(s1.getSearchedAt()));
            SearchHistory lastSearch = searches.get(0);
            return new String[]{lastSearch.getSource(), lastSearch.getDestination()};
        }

        // Fallback to recent booking
        List<Booking> bookings = bookingRepository.findByBookedByUser_UserId(userId);
        if (bookings != null && !bookings.isEmpty()) {
            bookings.sort((b1, b2) -> b2.getCreatedAt().compareTo(b1.getCreatedAt()));
            Booking lastBooking = bookings.get(0);
            return new String[]{
                    lastBooking.getTrip().getRoute().getSource(),
                    lastBooking.getTrip().getRoute().getDestination()
            };
        }

        return null;
    }

    private RecommendationDTO scoreTrip(TripDTO trip, UserPreference preference) {
        double score = 50.0; // Base score for being an active available trip on preferred route
        List<String> reasons = new ArrayList<>();
        reasons.add("Available on your frequently searched route");

        if (preference != null) {
            // Bus Type Preference
            if (preference.getPreferredBusType() != null && trip.getBusType() != null &&
                    trip.getBusType().equalsIgnoreCase(preference.getPreferredBusType())) {
                score += 15.0;
                reasons.add("Matches preferred bus type (" + preference.getPreferredBusType() + ")");
            }

            // AC Preference
            if (preference.getAcPreference() != null && trip.getAmenities() != null) {
                boolean isAc = trip.getAmenities().contains("AC");
                if (preference.getAcPreference() == isAc) {
                    score += 10.0;
                    reasons.add("Matches AC preference");
                }
            }

            // Price Range
            if (preference.getMinPrice() != null && trip.getBaseFare().compareTo(preference.getMinPrice()) >= 0) {
                score += 5.0;
            }
            if (preference.getMaxPrice() != null && trip.getBaseFare().compareTo(preference.getMaxPrice()) <= 0) {
                score += 10.0;
                reasons.add("Within your preferred budget");
            }

            // Operator Preference
            if (preference.getOperatorPreference() != null && trip.getOperatorName() != null &&
                    trip.getOperatorName().toLowerCase().contains(preference.getOperatorPreference().toLowerCase())) {
                score += 20.0;
                reasons.add("Operated by your preferred operator");
            }

            // Time preference (simplified)
            if (preference.getPreferredDepartureTime() != null && !preference.getPreferredDepartureTime().isBlank()) {
                String prefTime = preference.getPreferredDepartureTime().toLowerCase();
                int hour = trip.getDepartureTime().getHour();
                boolean matchesTime = false;
                if (prefTime.contains("morning") && hour >= 5 && hour < 12) matchesTime = true;
                if (prefTime.contains("afternoon") && hour >= 12 && hour < 17) matchesTime = true;
                if (prefTime.contains("evening") && hour >= 17 && hour < 21) matchesTime = true;
                if (prefTime.contains("night") && (hour >= 21 || hour < 5)) matchesTime = true;

                if (matchesTime) {
                    score += 15.0;
                    reasons.add("Departs at your preferred time of day");
                }
            }
        }

        String finalReason = String.join(" | ", reasons);
        return new RecommendationDTO(trip, score, finalReason);
    }
}
