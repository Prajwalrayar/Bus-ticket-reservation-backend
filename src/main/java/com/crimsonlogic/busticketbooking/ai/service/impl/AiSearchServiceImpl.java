package com.crimsonlogic.busticketbooking.ai.service.impl;

import com.crimsonlogic.busticketbooking.dto.TripSearchRequest;
import com.crimsonlogic.busticketbooking.entity.SearchHistory;
import com.crimsonlogic.busticketbooking.entity.User;
import com.crimsonlogic.busticketbooking.repository.SearchHistoryRepository;
import com.crimsonlogic.busticketbooking.repository.UserRepository;
import com.crimsonlogic.busticketbooking.ai.service.AiSearchService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class AiSearchServiceImpl implements AiSearchService {

    private final ChatClient chatClient;
    private final SearchHistoryRepository searchHistoryRepository;
    private final UserRepository userRepository;

    public AiSearchServiceImpl(ChatClient.Builder chatClientBuilder,
                               SearchHistoryRepository searchHistoryRepository,
                               UserRepository userRepository) {
        this.chatClient = chatClientBuilder.build();
        this.searchHistoryRepository = searchHistoryRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public TripSearchRequest extractSearchCriteria(String query, String userId) {

        BeanOutputConverter<TripSearchRequest> converter = new BeanOutputConverter<>(TripSearchRequest.class);
        String format = converter.getFormat();

        String systemPrompt = String.format(
                "You are an AI assistant for a bus ticket booking system. " +
                "Extract structured search criteria from the user's natural language query. " +
                "The current date is %s. Resolve relative dates like 'tomorrow' or 'next week' to exact dates. " +
                "For busType, use exact values: 'SEATER', 'SLEEPER', or 'SEMI_SLEEPER' if specified, otherwise leave null. " +
                "If 'AC' is mentioned, set isAc to true. If 'non-AC' is mentioned, set isAc to false. " +
                "Always return source and destination as lowercase city names (e.g., 'bangalore', not 'Bangalore'). " +
                "Return null for fields that are not explicitly requested, including travelDate if no date is mentioned. " +
                "Ensure source and destination are provided if they can be extracted. \n\n%s",
                LocalDate.now().toString(), format);

        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(query)
                .call()
                .content();

        // Strip markdown fences if Gemini wraps the JSON
        String cleanResponse = response.trim();
        if (cleanResponse.startsWith("```json")) cleanResponse = cleanResponse.substring(7);
        else if (cleanResponse.startsWith("```")) cleanResponse = cleanResponse.substring(3);
        if (cleanResponse.endsWith("```")) cleanResponse = cleanResponse.substring(0, cleanResponse.length() - 3);
        cleanResponse = cleanResponse.trim();

        TripSearchRequest searchRequest = converter.convert(cleanResponse);

        if (searchRequest == null || searchRequest.getSource() == null || searchRequest.getDestination() == null) {
            throw new IllegalArgumentException("Please specify both source and destination cities to search for buses.");
        }

        // Lowercase city names to match DB query (which uses LOWER() on columns)
        searchRequest.setSource(searchRequest.getSource().toLowerCase().trim());
        searchRequest.setDestination(searchRequest.getDestination().toLowerCase().trim());

        // Do NOT default to today - leave null so we search all future trips

        saveSearchHistory(query, searchRequest, userId);

        return searchRequest;
    }

    private void saveSearchHistory(String query, TripSearchRequest request, String userId) {
        SearchHistory searchHistory = new SearchHistory();
        searchHistory.setSource(request.getSource());
        searchHistory.setDestination(request.getDestination());
        searchHistory.setTravelDate(request.getTravelDate());
        searchHistory.setPassengerCount(1); // Default as it's not extracted
        searchHistory.setIsNaturalLanguage(true);
        searchHistory.setNaturalLanguageQuery(query);

        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            searchHistory.setUser(user);
        }

        searchHistoryRepository.save(searchHistory);
    }
}
