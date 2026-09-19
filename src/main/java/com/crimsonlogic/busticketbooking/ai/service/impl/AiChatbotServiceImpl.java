package com.crimsonlogic.busticketbooking.ai.service.impl;

import com.crimsonlogic.busticketbooking.dto.BookingCancelRequest;
import com.crimsonlogic.busticketbooking.dto.BookingDTO;
import com.crimsonlogic.busticketbooking.ai.dto.ChatRequest;
import com.crimsonlogic.busticketbooking.ai.dto.ChatResponse;
import com.crimsonlogic.busticketbooking.dto.TripDTO;
import com.crimsonlogic.busticketbooking.dto.TripSearchRequest;
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

import java.time.LocalDate;
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
    private final com.crimsonlogic.busticketbooking.service.AdminService adminService;
    private final com.crimsonlogic.busticketbooking.service.OperatorService operatorService;

    public AiChatbotServiceImpl(ChatClient.Builder chatClientBuilder,
                                AiSearchService aiSearchService,
                                TripService tripService,
                                BookingService bookingService,
                                CancellationService cancellationService,
                                AiRecommendationService aiRecommendationService,
                                AiDynamicPricingService aiDynamicPricingService,
                                com.crimsonlogic.busticketbooking.service.AdminService adminService,
                                com.crimsonlogic.busticketbooking.service.OperatorService operatorService) {
        this.chatClient = chatClientBuilder.build();
        this.aiSearchService = aiSearchService;
        this.tripService = tripService;
        this.bookingService = bookingService;
        this.cancellationService = cancellationService;
        this.aiRecommendationService = aiRecommendationService;
        this.aiDynamicPricingService = aiDynamicPricingService;
        this.adminService = adminService;
        this.operatorService = operatorService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    @Override
    public ChatResponse processChatMessage(String userId, String userRole, ChatRequest request) {
        // Step 1: Extract intent with a single API call
        ChatbotExtractionResult extraction = extractIntent(request.getMessage(), userRole);
        log.info("Chatbot extracted intent: {} with entities: {}", extraction.getIntent(), extraction.getEntities());

        // Step 2: Route to backend services
        Object backendResult = routeIntent(userId, extraction.getIntent(), extraction.getEntities(), request.getMessage());

        // Step 3: Format final response WITHOUT another AI call (saves rate limit)
        String finalResponse = formatResponse(backendResult, extraction.getIntent(), extraction.getEntities());

        return new ChatResponse(finalResponse);
    }

    private ChatbotExtractionResult extractIntent(String message, String userRole) {
        String lowerMsg = message.toLowerCase().trim();
        // Rule-based bypass for common intents to save AI API quota (15 RPM limit)
        if (lowerMsg.contains("show my booking") || lowerMsg.contains("my bookings") || 
            lowerMsg.contains("view booking") || lowerMsg.equals("bookings") || lowerMsg.contains("show bookings")) {
            ChatbotExtractionResult result = new ChatbotExtractionResult();
            result.setIntent("GET_BOOKING");
            result.setEntities(new java.util.HashMap<>());
            return result;
        }

        if (lowerMsg.contains("search bus") || lowerMsg.contains("buses from") || lowerMsg.contains("bus from") || lowerMsg.contains("find buses")) {
            java.util.regex.Pattern pExact = java.util.regex.Pattern.compile("from\\s+([a-zA-Z]+)\\s+to\\s+([a-zA-Z]+)\\s+on\\s+(\\d{4}-\\d{2}-\\d{2})");
            java.util.regex.Matcher mExact = pExact.matcher(lowerMsg);
            if (mExact.find()) {
                ChatbotExtractionResult result = new ChatbotExtractionResult();
                result.setIntent("SEARCH_BUS");
                java.util.Map<String, String> map = new java.util.HashMap<>();
                map.put("source", mExact.group(1));
                map.put("destination", mExact.group(2));
                map.put("date", mExact.group(3));
                result.setEntities(map);
                return result;
            }
            
            java.util.regex.Pattern pLoose = java.util.regex.Pattern.compile("from\\s+([a-zA-Z]+)\\s+to\\s+([a-zA-Z]+)(.*)");
            java.util.regex.Matcher mLoose = pLoose.matcher(lowerMsg);
            if (mLoose.find()) {
                String extra = mLoose.group(3).trim();
                if (extra.isEmpty()) {
                    ChatbotExtractionResult result = new ChatbotExtractionResult();
                    result.setIntent("SEARCH_BUS");
                    java.util.Map<String, String> map = new java.util.HashMap<>();
                    map.put("source", mLoose.group(1));
                    map.put("destination", mLoose.group(2));
                    result.setEntities(map);
                    return result;
                }
            }
        }

        if (lowerMsg.contains("recommend") || lowerMsg.contains("suggest")) {
            ChatbotExtractionResult result = new ChatbotExtractionResult();
            result.setIntent("RECOMMEND_BUS");
            result.setEntities(new java.util.HashMap<>());
            return result;
        }

        if (lowerMsg.contains("revenue") || lowerMsg.contains("dashboard") || lowerMsg.contains("kpi") || 
            lowerMsg.contains("total bookings") || lowerMsg.contains("total users") || lowerMsg.contains("total buses")) {
            ChatbotExtractionResult result = new ChatbotExtractionResult();
            result.setIntent("VIEW_DASHBOARD");
            result.setEntities(new java.util.HashMap<>());
            return result;
        }

        if (lowerMsg.startsWith("cancel trip")) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("cancel trip\\s+([A-Za-z0-9_]+)");
            java.util.regex.Matcher m = p.matcher(lowerMsg);
            if (m.find()) {
                ChatbotExtractionResult result = new ChatbotExtractionResult();
                result.setIntent("CANCEL_TRIP");
                java.util.Map<String, String> map = new java.util.HashMap<>();
                map.put("tripId", m.group(1).toUpperCase());
                result.setEntities(map);
                return result;
            }
        }

        BeanOutputConverter<ChatbotExtractionResult> converter = new BeanOutputConverter<>(ChatbotExtractionResult.class);
        String format = converter.getFormat();

        String allowedIntents;
        String roleSpecificInstructions;

        if ("ROLE_ADMIN".equals(userRole) || "ROLE_BUS_OPERATOR".equals(userRole)) {
            allowedIntents = "VIEW_DASHBOARD, CANCEL_TRIP, SEARCH_BUS, FAQ, UNKNOWN";
            roleSpecificInstructions = "If the user asks for revenue, KPIs, or dashboard status, use VIEW_DASHBOARD. " +
                                       "If the user asks to cancel a trip (e.g. 'cancel trip TRP123'), use CANCEL_TRIP and extract 'tripId'.";
        } else {
            allowedIntents = "SEARCH_BUS, CHECK_SEATS, GET_BOOKING, CANCEL_BOOKING, RECOMMEND_BUS, CHECK_FARE, FAQ, UNKNOWN";
            roleSpecificInstructions = "If the user asks to see their bookings (even without a reference ID), use GET_BOOKING. " +
                                       "If the user asks for operator/admin data (like revenue/dashboard or cancel trip), return UNKNOWN.";
        }

        String systemPrompt = String.format(
                "You are an AI intent extractor for a bus ticket booking system. " +
                "Extract the user's intent into one of the following exact strings: %s. " +
                "Also extract relevant entities from the message into a map: " +
                "  'source' (city name lowercase), 'destination' (city name lowercase), " +
                "  'date' (ISO date format YYYY-MM-DD if mentioned, today=%s), " +
                "  'bookingReference', 'tripId', 'reason'. " +
                "If the user asks a procedural question (e.g., how to book, how to cancel), use FAQ. " +
                "%s " +
                "If intent is unclear, use UNKNOWN. " +
                "Return ONLY the JSON, NO markdown, NO extra text.\n\n%s",
                allowedIntents, LocalDate.now().toString(), roleSpecificInstructions, format);

        String response;
        try {
            response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(message)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI API error during intent extraction: {}", e.getMessage());
            ChatbotExtractionResult fallback = new ChatbotExtractionResult();
            fallback.setIntent("UNKNOWN");
            return fallback;
        }

        // Strip markdown fences if present
        String clean = response.trim();
        if (clean.startsWith("```json")) clean = clean.substring(7);
        else if (clean.startsWith("```")) clean = clean.substring(3);
        if (clean.endsWith("```")) clean = clean.substring(0, clean.length() - 3);
        clean = clean.trim();

        try {
            ChatbotExtractionResult result = converter.convert(clean);
            if (result == null) {
                ChatbotExtractionResult fallback = new ChatbotExtractionResult();
                fallback.setIntent("UNKNOWN");
                return fallback;
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to parse intent extraction response: {}", clean, e);
            ChatbotExtractionResult fallback = new ChatbotExtractionResult();
            fallback.setIntent("UNKNOWN");
            return fallback;
        }
    }

    private Object routeIntent(String userId, String intent, Map<String, String> entities, String originalMessage) {
        try {
            switch (intent) {
                case "SEARCH_BUS":
                    // Build search request from extracted entities - no extra AI call needed
                    String source = entities != null ? entities.get("source") : null;
                    String destination = entities != null ? entities.get("destination") : null;

                    if (source == null || destination == null) {
                        return "CLARIFY:Please tell me which city you want to travel FROM and TO. For example: 'Search buses from Bangalore to Chennai'";
                    }

                    TripSearchRequest searchReq = new TripSearchRequest();
                    searchReq.setSource(source.toLowerCase().trim());
                    searchReq.setDestination(destination.toLowerCase().trim());

                    // Set date if provided
                    String dateStr = entities != null ? entities.get("date") : null;
                    if (dateStr != null && !dateStr.isEmpty()) {
                        try {
                            searchReq.setTravelDate(LocalDate.parse(dateStr));
                        } catch (Exception e) {
                            log.warn("Could not parse date: {}", dateStr);
                            return "Please select a valid travel date:\n[DATE_PICKER:source=" + source + ",destination=" + destination + "]";
                        }
                    } else {
                        return "Please select your travel date:\n[DATE_PICKER:source=" + source + ",destination=" + destination + "]";
                    }

                    List<TripDTO> trips = tripService.searchTrips(searchReq);
                    if (trips.isEmpty()) {
                        return "NO_TRIPS_FOUND:" + searchReq.getSource() + ":" + searchReq.getDestination();
                    }
                    return trips;

                case "CHECK_SEATS":
                    String tripId = entities != null ? entities.get("tripId") : null;
                    if (tripId != null) {
                        return tripService.getTripById(tripId).getAvailableSeats();
                    }
                    return "Error: Please specify a Trip ID to check seats.";

                case "GET_BOOKING":
                    String bookingRef = entities != null ? entities.get("bookingReference") : null;
                    if (bookingRef == null || bookingRef.isEmpty()) {
                        List<BookingDTO> userBookings = bookingService.getMyBookings();
                        if (userBookings.isEmpty()) {
                            return "NO_BOOKINGS_FOUND";
                        }
                        return userBookings;
                    } else {
                        BookingDTO booking = bookingService.getBookingByReference(bookingRef);
                        boolean isOwner = bookingService.getMyBookings().stream()
                                .anyMatch(b -> b.getBookingReference().equals(bookingRef));
                        if (!isOwner) {
                            return "Error: You can only view your own bookings.";
                        }
                        return booking;
                    }

                case "CANCEL_BOOKING":
                    String cancelRef = entities != null ? entities.get("bookingReference") : null;
                    String reason = (entities != null) ? entities.getOrDefault("reason", "Cancelled by user via Chatbot") : "Cancelled by user via Chatbot";
                    if (cancelRef == null) {
                        return "CLARIFY:Please provide your booking reference number to cancel your booking.";
                    }
                    BookingDTO bookingToCancel = bookingService.getBookingByReference(cancelRef);
                    boolean isOwnerCancel = bookingService.getMyBookings().stream()
                            .anyMatch(b -> b.getBookingReference().equals(cancelRef));
                    if (!isOwnerCancel) {
                        return "Error: You can only cancel your own bookings.";
                    }
                    BookingCancelRequest cancelReq = new BookingCancelRequest();
                    cancelReq.setCancellationReason(reason);
                    return bookingService.cancelBooking(bookingToCancel.getBookingId(), cancelReq);

                case "RECOMMEND_BUS":
                    return aiRecommendationService.getRecommendations(userId);

                case "CHECK_FARE":
                    String fareTripId = entities != null ? entities.get("tripId") : null;
                    if (fareTripId != null) {
                        return aiDynamicPricingService.simulatePricing(fareTripId);
                    }
                    return "Error: Please specify a Trip ID to check fare.";

                case "VIEW_DASHBOARD":
                    String role = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority();
                    if ("ROLE_ADMIN".equals(role)) {
                        return adminService.getDashboardKPIs();
                    } else if ("ROLE_BUS_OPERATOR".equals(role)) {
                        return operatorService.getDashboardKPIs();
                    } else {
                        return "Error: You are not authorized to view the dashboard.";
                    }

                case "CANCEL_TRIP":
                    String tripIdToCancel = entities != null ? entities.get("tripId") : null;
                    if (tripIdToCancel == null) {
                        return "Please provide the Trip ID you want to cancel (e.g. 'Cancel trip TRP123').";
                    }
                    try {
                        tripService.cancelTrip(tripIdToCancel, "Cancelled via AI Chatbot by Admin/Operator");
                        return "SUCCESS: Trip " + tripIdToCancel + " has been successfully cancelled.";
                    } catch (Exception e) {
                        return "Error: Failed to cancel trip. " + e.getMessage();
                    }

                case "FAQ":
                    String systemInstructions = "You are a friendly customer support AI for our bus ticket booking system.\n" +
                            "Here is how our system works:\n" +
                            "- To book a bus, users can search for a bus (e.g., 'buses from Bangalore to Chennai'), select a date from the calendar, pick seats, and complete payment.\n" +
                            "- To view bookings, users can say 'show my bookings'.\n" +
                            "- To cancel a booking, users can say 'cancel my booking [ReferenceNumber]'.\n" +
                            "- For recommendations, users can say 'recommend a bus'.\n" +
                            "Answer the user's procedural question concisely and nicely using this knowledge.";
                    try {
                        return chatClient.prompt().system(systemInstructions).user(originalMessage).call().content();
                    } catch (Exception e) {
                        log.error("AI API error during FAQ generation: {}", e.getMessage());
                        return "Sorry, I am currently unable to answer that. Please try again later.";
                    }

                case "UNKNOWN":
                default:
                    return "HELP";
            }
        } catch (Exception e) {
            log.error("Error executing backend service for intent {}: {}", intent, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Format the backend result into a human-readable response WITHOUT making another AI API call.
     * This saves our rate-limited Gemini quota (only 5 RPM on free tier).
     */
    private String formatResponse(Object backendData, String intent, Map<String, String> entities) {
        if (backendData == null) {
            return "I couldn't find any information for your request. Please try again.";
        }

        String data = backendData.toString();

        // Handle special marker strings
        if (data.startsWith("CLARIFY:")) {
            return data.substring(8);
        }

        if (data.startsWith("NO_TRIPS_FOUND:")) {
            String[] parts = data.split(":");
            String src = parts.length > 1 ? capitalize(parts[1]) : "your source";
            String dst = parts.length > 2 ? capitalize(parts[2]) : "your destination";
            return String.format(
                "🔍 I searched for buses from **%s** to **%s**, but no upcoming trips are currently available on this route.\n\n" +
                "You can try:\n" +
                "- Searching for a specific future date (e.g., 'buses from %s to %s on 20 Sep')\n" +
                "- Checking back later as new trips may be added\n\n" +
                "Is there anything else I can help you with?", src, dst, src, dst);
        }

        if (data.equals("NO_BOOKINGS_FOUND")) {
            return "You currently don't have any upcoming bookings.";
        }

        if (data.equals("HELP")) {
            return "👋 Hello! I'm your AI Travel Assistant. Here's what I can do for you:\n\n" +
                   "🔍 **Search buses** — *'Buses from Bangalore to Chennai'*\n" +
                   "📋 **View booking** — *'Show my booking BK123456'*\n" +
                   "❌ **Cancel booking** — *'Cancel my booking BK123456'*\n" +
                   "⭐ **Recommendations** — *'Recommend a bus for me'*\n\n" +
                   "How can I help you today?";
        }

        if (data.startsWith("Error:")) {
            return "⚠️ " + data.substring(6).trim() + " Please try again or contact support if the issue persists.";
        }

        // Handle AdminDashboardDTO
        if (backendData instanceof com.crimsonlogic.busticketbooking.dto.AdminDashboardDTO) {
            com.crimsonlogic.busticketbooking.dto.AdminDashboardDTO dash = (com.crimsonlogic.busticketbooking.dto.AdminDashboardDTO) backendData;
            return String.format(
                "📊 **Dashboard KPIs**\n\n" +
                "💰 **Total Revenue:** ₹%.2f\n" +
                "🎫 **Total Bookings:** %d (Confirmed: %d, Cancelled: %d)\n" +
                "👤 **Total Users:** %d\n" +
                "🚌 **Total Buses:** %d",
                dash.getTotalRevenue(),
                dash.getTotalBookings(), dash.getConfirmedBookings(), dash.getCancelledBookings(),
                dash.getTotalUsers(),
                dash.getTotalBuses()
            );
        }

        if (data.startsWith("SUCCESS:")) {
            return "✅ " + data.substring(8).trim();
        }

        // Handle List of trips
        if (backendData instanceof List && !((List<?>) backendData).isEmpty() && ((List<?>) backendData).get(0) instanceof com.crimsonlogic.busticketbooking.dto.TripDTO) {
            List<?> list = (List<?>) backendData;

            StringBuilder sb = new StringBuilder();
            sb.append("🚌 Here are the available buses I found:\n\n");

            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                try {
                    String json = objectMapper.writeValueAsString(item);
                    com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(json);

                    // Skip buses with 0 available seats
                    if (node.has("availableSeats") && node.get("availableSeats").asInt() == 0) {
                        continue;
                    }

                    sb.append("**").append(i + 1).append(". ");
                    // Use operatorName as the bus/service name
                    if (node.has("operatorName") && !node.get("operatorName").isNull()) {
                        sb.append(node.get("operatorName").asText());
                    } else if (node.has("busRegistrationNumber") && !node.get("busRegistrationNumber").isNull()) {
                        sb.append(node.get("busRegistrationNumber").asText());
                    } else {
                        sb.append("Bus Service");
                    }
                    sb.append("**");

                    if (node.has("busType") && !node.get("busType").isNull()) {
                        sb.append(" (").append(node.get("busType").asText()).append(")");
                    }
                    // AC / Non-AC indicator — default to Non-AC if not present or false
                    boolean isAc = node.has("ac") && node.get("ac").asBoolean();
                    sb.append(isAc ? " ❄️ AC" : " 🌬️ Non-AC");
                    sb.append("\n");

                    if (node.has("source") && node.has("destination")) {
                        sb.append("   📍 ").append(capitalize(node.get("source").asText()))
                          .append(" → ").append(capitalize(node.get("destination").asText())).append("\n");
                    }
                    if (node.has("baseFare") && !node.get("baseFare").isNull()) {
                        sb.append("   💰 Fare: ₹").append(node.get("baseFare").asText()).append("\n");
                    }
                    if (node.has("availableSeats")) {
                        sb.append("   💺 Available Seats: ").append(node.get("availableSeats").asText()).append("\n");
                    }
                    sb.append("\n");
                } catch (Exception e) {
                    sb.append(item.toString()).append("\n");
                }
            }
            return sb.toString().trim();
        }

        // Handle List of Bookings
        if (backendData instanceof List && !((List<?>) backendData).isEmpty() && ((List<?>) backendData).get(0) instanceof BookingDTO) {
            List<?> list = (List<?>) backendData;
            StringBuilder sb = new StringBuilder();
            sb.append("📋 **Here are your bookings:**\n\n");
            
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                try {
                    String json = objectMapper.writeValueAsString(item);
                    com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(json);
                    
                    // Fetch trip details for source, destination and date
                    String tripId = node.has("tripId") ? node.get("tripId").asText() : null;
                    String source = "Unknown";
                    String destination = "Unknown";
                    String travelDateStr = "Unknown";
                    boolean isUpcoming = false;
                    
                    if (tripId != null) {
                        try {
                            com.crimsonlogic.busticketbooking.dto.TripDTO trip = tripService.getTripById(tripId);
                            if (trip != null) {
                                source = capitalize(trip.getSource());
                                destination = capitalize(trip.getDestination());
                                if (trip.getTravelDate() != null) {
                                    travelDateStr = trip.getTravelDate().toString();
                                    isUpcoming = trip.getTravelDate().isAfter(LocalDate.now()) || trip.getTravelDate().isEqual(LocalDate.now());
                                }
                            }
                        } catch (Exception ex) {
                            log.warn("Could not fetch trip for booking: {}", tripId);
                        }
                    }

                    sb.append("**").append(i + 1).append(". ");
                    if (node.has("bookingReference")) sb.append(node.get("bookingReference").asText());
                    if (isUpcoming && node.has("bookingStatus") && "CONFIRMED".equals(node.get("bookingStatus").asText())) {
                        sb.append(" (upcoming)");
                    }
                    sb.append("**\n");
                    
                    if (node.has("bookingStatus")) sb.append("   📌 Status: ").append(node.get("bookingStatus").asText()).append("\n");
                    sb.append("   from: ").append(source).append(" -> to: ").append(destination).append("\n");
                    sb.append("   date: ").append(travelDateStr).append("\n\n");
                } catch (Exception e) {
                    sb.append(item.toString()).append("\n");
                }
            }
            return sb.toString().trim();
        }

        // Handle BookingDTO
        try {
            String json = objectMapper.writeValueAsString(backendData);
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(json);

            if (node.has("bookingReference")) {
                StringBuilder sb = new StringBuilder();
                sb.append("📋 **Booking Details**\n\n");
                if (node.has("bookingReference")) sb.append("🔖 **Reference:** ").append(node.get("bookingReference").asText()).append("\n");
                if (node.has("bookingStatus")) sb.append("📌 **Status:** ").append(node.get("bookingStatus").asText()).append("\n");
                if (node.has("source") && node.has("destination")) {
                    sb.append("📍 **Route:** ").append(capitalize(node.get("source").asText()))
                      .append(" → ").append(capitalize(node.get("destination").asText())).append("\n");
                }
                if (node.has("travelDate")) sb.append("📅 **Date:** ").append(node.get("travelDate").asText()).append("\n");
                if (node.has("totalSeats")) sb.append("💺 **Seats:** ").append(node.get("totalSeats").asText()).append("\n");
                if (node.has("totalAmount")) sb.append("💰 **Total Amount:** ₹").append(node.get("totalAmount").asText()).append("\n");
                return sb.toString();
            }
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize backendData for formatting: {}", e.getMessage());
        }

        // Fallback: just return the string representation
        return data;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
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
