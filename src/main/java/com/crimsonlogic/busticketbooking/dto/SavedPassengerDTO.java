package com.crimsonlogic.busticketbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavedPassengerDTO {

    private String savedPassengerId;

    private String passengerName;

    private Integer age;

    private String gender;

    private String idType;

    private String idNumber;

    private String contactNumber;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
