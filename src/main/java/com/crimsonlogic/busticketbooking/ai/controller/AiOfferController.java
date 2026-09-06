package com.crimsonlogic.busticketbooking.ai.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.ai.dto.PersonalizedOfferDTO;
import com.crimsonlogic.busticketbooking.ai.service.AiOfferGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai/offers")
@RequiredArgsConstructor
public class AiOfferController {

    private final AiOfferGenerationService aiOfferGenerationService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<PersonalizedOfferDTO>>> getUserOffers(@PathVariable String userId) {
        List<PersonalizedOfferDTO> offers = aiOfferGenerationService.getOrGenerateOffersForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(offers));
    }
}
