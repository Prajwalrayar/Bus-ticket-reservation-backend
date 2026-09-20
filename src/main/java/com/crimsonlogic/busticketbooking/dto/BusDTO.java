package com.crimsonlogic.busticketbooking.dto;

import com.crimsonlogic.busticketbooking.enums.BusType;
import com.crimsonlogic.busticketbooking.enums.BusActivationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusDTO {

    private String busId;

    private String registrationNumber;

    private BusType busType;

    private Set<String> amenities;

    private Boolean isActive;

    private String operatorCompanyName;

    private Boolean petsAllowed;

    private String baggagePolicy;

    private LocalDate lastTripDate;

    private BusActivationStatus activationRequestStatus;

    private String activationRequestNote;

    private BigDecimal compensationAmount;

    private String adminRejectionNote;

    private LocalDateTime activationRequestedAt;

    private LocalDateTime activationApprovedAt;
}
