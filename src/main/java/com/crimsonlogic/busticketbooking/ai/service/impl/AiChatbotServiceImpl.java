package com.crimsonlogic.busticketbooking.ai.service.impl;

import com.crimsonlogic.busticketbooking.dto.BookingCancelRequest;
import com.crimsonlogic.busticketbooking.dto.BookingDTO;
import com.crimsonlogic.busticketbooking.dto.CancellationDTO;
import com.crimsonlogic.busticketbooking.ai.dto.ChatRequest;
import com.crimsonlogic.busticketbooking.ai.dto.ChatResponse;
import com.crimsonlogic.busticketbooking.ai.dto.DynamicPricingDTO;
import com.crimsonlogic.busticketbooking.ai.dto.RecommendationDTO;
import com.crimsonlogic.busticketbooking.dto.TripDTO;
import com.crimsonlogic.busticketbooking.dto.TripSearchRequest;
import com.crimsonlogic.busticketbooking.dto.TripSeatDTO;
import com.crimsonlogic.busticketbooking.ai.service.AiChatbotService;
import com.crimsonlogic.busticketbooking.ai.service.AiDynamicPricingService;
import com.crimsonlogic.busticketbooking.ai.service.AiRecommendationService;
import com.crimsonlogic.busticketbooking.ai.service.AiSearchService;
import com.crimsonlogic.busticketbooking.service.BookingService;
import com.crimsonlogic.busticketbooking.service.CancellationService;
import com.crimsonlogic.busticketbooking.service.TripService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiChatbotServiceImpl implements AiChatbotService {

    private final ChatClient chatClient;
    private final AiSearchService aiSearchService;
    private final TripService tripService;
    private final BookingService bookingService;
    private final CancellationService cancellationService;
    private final AiRecommendationService aiRecommendationService;
    private final AiDynamicPricingService aiDynamicPricingService;
    private final ObjectMapper objectMapper;

    public AiChatbotServiceImpl(ChatClient.Builder chatClientBuilder,
                                AiSearchService aiSearchService,
                                TripService tripService,
                                BookingService bookingService,
                                CancellationService cancellationService,
                                AiRecommendationService aiRecommendationService,
                                AiDynamicPricingService aiDynamicPricingService) {
        this.chatClient = chatClientBuilder.build();
        this.aiSearchService = aiSearchService;
        this.tripService = tripService;
        this.bookingService = bookingService;
        this.cancellationService = cancellationService;
        this.aiRecommendationService = aiRecommendationService;
        this.aiDynamicPricingService = aiDynamicPricingService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // Support for dates
    }

    @Override
    public ChatResponse processChatMessage(String userId, ChatRequest request) {
        // Step 1: Intent Extraction
        ChatbotExtractionResult extraction = extractIntent(request.getMessage());
        log.info("Chatbot extracted intent: {} with entities: {}", extraction.getIntent(), extraction.getEntities());

        // Step 2: Service Routing
        Object backendResult = routeIntent(userId, extraction.getIntent(), extraction.getEntities(), request.getMessage());

        // Step 3: Natural Language Generation
        String finalResponse = generateConversationalResponse(request.getMessage(), backendResult);

        return new ChatResponse(finalResponse);
    }

    private ChatbotExtractionResult extractIntent(String message) {
        BeanOutputConverter<ChatbotExtractionResult> converter = new BeanOutputConverter<>(ChatbotExtractionResult.class);
        String format = converter.getFormat();

        String systemPrompt = String.format(
                "You are an AI intent extractor for a bus booking system. " +
                "Extract the user's intent into one of the following exact strings: " +
                "SEARCH_BUS, CHECK_SEATS, SEARCH_ROUTE, COMPARE_BUSES, GET_BOOKING, CANCEL_BOOKING, RECOMMEND_BUS, CHECK_FARE, UNKNOWN. " +
                "Also extract relevant entities (e.g., 'tripId', 'bookingReference', 'source', 'destination', 'date', 'reason') into a map. " +
                "If the intent is unclear, use UNKNOWN. " +
                "Return exactly the requested JSON format.\n\n%%s", format);

        String response;
        try {
            response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(message)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI service error during intent extraction: {}", e.getMessage());
            ChatbotExtractionResult fallback = new ChatbotExtractionResult();
            fallback.setIntent("UNKNOWN");
            return fallback;
        }

        try {
            return converter.convert(response);
        } catch (Exception e) {
            log.error("Failed to parse intent extraction: {}", response, e);
            ChatbotExtractionResult fallback = new ChatbotExtractionResult();
            fallback.setIntent("UNKNOWN");
            return fallback;
        }
    }

    private Object routeIntent(String userId, String intent, Map<String, String> entities, String originalMessage) {
        try {
            switch (intent) {
                case "SEARCH_BUS":
                case "COMPARE_BUSES":
                case "SEARCH_ROUTE":
                    // Reuse Phase 1 search extraction logic implicitly by just passing the whole message, or use the entities
                    TripSearchRequest searchRequest = aiSearchService.extractSearchCriteria(originalMessage, userId);
                    return tripService.searchTrips(searchRequest);

                case "CHECK_SEATS":
                    String tripId = entities.get("tripId");
                    if (tripId != null) {
                        return tripService.getTripById(tripId).getAvailableSeats();
                    }
                    return "Error: Please specify a valid Trip ID to check seats.";

                case "GET_BOOKING":
                    String bookingRef = entities.get("bookingReference");
                    if (bookingRef == null) return "Error: Please provide your booking reference.";
                    
                    BookingDTO booking = bookingService.getBookingByReference(bookingRef);
                    // Security check
                    if (!booking.getUserId().equals(userId)) {
                        return "Error: Unauthorized. You can only view your own bookings.";
                    }
                    return booking;

                case "CANCEL_BOOKING":
                    String cancelRef = entities.get("bookingReference");
                    String reason = entities.getOrDefault("reason", "Cancelled by user via Chatbot");
                    if (cancelRef == null) return "Error: Please provide your booking reference to cancel.";
                    
                    BookingDTO bookingToCancel = bookingService.getBookingByReference(cancelRef);
                    // Security check
                    if (!bookingToCancel.getUserId().equals(userId)) {
                        return "Error: Unauthorized. You can only cancel your own bookings.";
                    }
                    
                    BookingCancelRequest cancelReq = new BookingCancelRequest();
                    cancelReq.setCancellationReason(reason);
                    return bookingService.cancelBooking(bookingToCancel.getBookingId(), cancelReq);

                case "RECOMMEND_BUS":
                    return aiRecommendationService.getRecommendations(userId);

                case "CHECK_FARE":
                    String fareTripId = entities.get("tripId");
                    if (fareTripId != null) {
                        return aiDynamicPricingService.simulatePricing(fareTripId);
                    }
                    return "Error: Please specify a valid Trip ID to check the fare.";

                case "UNKNOWN":
                default:
                    return "I am not sure how to help with that. You can ask me to search for buses, view a booking, cancel a booking, or recommend trips!";
            }
        } catch (Exception e) {
            log.error("Error executing backend service for intent {}: {}", intent, e.getMessage());
            return "An error occurred while processing your request: " + e.getMessage();
        }
    }

    private String generateConversationalResponse(String originalMessage, Object backendData) {
        String dataJson = "";
        try {
            dataJson = objectMapper.writeValueAsString(backendData);
        } catch (JsonProcessingException e) {
            dataJson = backendData.toString();
        }

        String systemPrompt = "You are a friendly, helpful AI travel assistant. " +
                "The user asked a question, and the backend system returned the following data (in JSON format). " +
                "Write a natural, conversational response to the user based ONLY on this data. " +
                "Do NOT expose sensitive UUIDs or raw technical JSON to the user; format it nicely (e.g. bold bus names, lists). " +
                "If the data contains an error message, politely explain the issue to the user.\n\n" +
                "Data: " + dataJson;

        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user("User message: " + originalMessage)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI service error during NLG: {}", e.getMessage());
            return "Here is what I found for you:\n\n" + dataJson;
        }
    }

    // Helper class for LLM extraction
    public static class ChatbotExtractionResult {
        private String intent;
        private Map<String, String> entities;

        public ChatbotExtractionResult() {}

        public String getIntent() { return intent; }
        public void setIntent(String intent) { this.intent = intent; }
        public Map<String, String> getEntities() { return entities; }
        public void setEntities(Map<String, String> entities) { this.entities = entities; }
    }
}
