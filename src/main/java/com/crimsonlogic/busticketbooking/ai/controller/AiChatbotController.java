package com.crimsonlogic.busticketbooking.ai.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.ai.dto.ChatRequest;
import com.crimsonlogic.busticketbooking.ai.dto.ChatResponse;
import com.crimsonlogic.busticketbooking.ai.service.AiChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatbotController {

    private final AiChatbotService aiChatbotService;

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @RequestBody ChatRequest request,
            Authentication authentication) {
        
        // Use the authenticated user's ID for secure routing
        String userId = authentication.getName();
        
        ChatResponse response = aiChatbotService.processChatMessage(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
