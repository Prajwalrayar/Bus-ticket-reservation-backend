package com.crimsonlogic.busticketbooking.dto;

import com.crimsonlogic.busticketbooking.validation.ValidEmail;
import com.crimsonlogic.busticketbooking.validation.ValidIndianPhone;
import com.crimsonlogic.busticketbooking.validation.ValidName;
import com.crimsonlogic.busticketbooking.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @ValidName
    private String fullName;

    @ValidEmail
    private String email;

    @ValidIndianPhone
    private String mobileNumber;

    @ValidPassword
    private String password;

    @NotBlank
    private String confirmPassword;

    private String address;
}
