package com.crimsonlogic.busticketbooking.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiSearchRequest {

    @NotBlank(message = "Query must not be empty")
    private String query;
}
