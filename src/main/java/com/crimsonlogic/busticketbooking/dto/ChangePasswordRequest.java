package com.crimsonlogic.busticketbooking.dto;

import jakarta.validation.constraints.NotBlank;
import com.crimsonlogic.busticketbooking.validation.ValidPassword;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    @NotBlank
    private String currentPassword;

    @ValidPassword
    private String newPassword;

    @NotBlank
    private String confirmPassword;
}