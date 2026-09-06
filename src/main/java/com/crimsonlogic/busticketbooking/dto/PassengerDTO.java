package com.crimsonlogic.busticketbooking.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassengerDTO {

    @NotBlank
    @Size(max = 100)
    private String passengerName;

    @NotNull
    @Min(1)
    @Max(120)
    private Integer age;

    @NotBlank
    private String gender;

    private String idType;

    @Size(max = 30)
    private String idNumber;

    @Size(max = 15)
    private String contactNumber;

    @NotBlank
    private String seatNumber;

    private Boolean isPrimary;
}
