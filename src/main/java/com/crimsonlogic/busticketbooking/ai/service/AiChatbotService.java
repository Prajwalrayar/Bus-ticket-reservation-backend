package com.crimsonlogic.busticketbooking.ai.service;

import com.crimsonlogic.busticketbooking.ai.dto.ChatRequest;
import com.crimsonlogic.busticketbooking.ai.dto.ChatResponse;

public interface AiChatbotService {
    
    /**
     * Processes a natural language chatbot request for a specific user.
     * @param userId The authenticated user ID.
     * @param request The chat request containing the user's message.
     * @return ChatResponse containing the natural language reply.
     */
    ChatResponse processChatMessage(String userId, ChatRequest request);
}
