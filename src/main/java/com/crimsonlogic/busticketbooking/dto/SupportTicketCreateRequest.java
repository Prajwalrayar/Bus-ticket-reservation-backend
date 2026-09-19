package com.crimsonlogic.busticketbooking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SupportTicketCreateRequest {

    @NotBlank(message = "Issue category is required")
    @Size(max = 50, message = "Issue category cannot exceed 50 characters")
    private String issueCategory;

    @NotBlank(message = "Issue type is required")
    @Size(max = 100, message = "Issue type cannot exceed 100 characters")
    private String issueType;

    @NotBlank(message = "Issue subject is required")
    @Size(max = 100, message = "Issue subject cannot exceed 100 characters")
    private String issueSubject;

    @NotBlank(message = "Issue description is required")
    @Size(max = 1000, message = "Issue description cannot exceed 1000 characters")
    private String issueDescription;

    private String operatorId;

    @Size(max = 50, message = "Booking reference cannot exceed 50 characters")
    private String bookingReference;

    private String attachmentPath;
}
