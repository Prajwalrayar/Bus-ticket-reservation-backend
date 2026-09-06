package com.crimsonlogic.busticketbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;

    private String tokenType = "Bearer";

    private String userId;

    private String userName;

    private String userEmail;

    private List<String> roles;

    private LocalDateTime expiresAt;

    private Boolean isTemporaryPassword;
}
