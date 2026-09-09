package com.crimsonlogic.busticketbooking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FareLocationCreateRequest {

    @NotBlank
    private String name;

    private String description;
}
