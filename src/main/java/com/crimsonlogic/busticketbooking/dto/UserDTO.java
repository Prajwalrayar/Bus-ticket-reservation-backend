package com.crimsonlogic.busticketbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private String userId;

    private String userName;

    private String userEmail;

    private String mobileNumber;

    private Boolean isActive;

    private Boolean isVerified;

    private List<String> roleNames;

    private String companyName;

    private LocalDateTime createdAt;

    private Boolean isTemporaryPassword;
}
