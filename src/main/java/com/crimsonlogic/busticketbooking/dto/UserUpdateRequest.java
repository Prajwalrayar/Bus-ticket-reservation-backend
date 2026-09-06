package com.crimsonlogic.busticketbooking.dto;

import com.crimsonlogic.busticketbooking.validation.ValidIndianPhone;
import com.crimsonlogic.busticketbooking.validation.ValidName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    @ValidName
    private String userName;

    @ValidIndianPhone
    private String mobileNumber;
}