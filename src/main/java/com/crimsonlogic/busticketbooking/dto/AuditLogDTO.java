package com.crimsonlogic.busticketbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {

    private String auditLogId;

    private String action;

    private String entityName;

    private String entityReference;

    private String description;

    private String performedByUserName;

    private LocalDateTime createdAt;
}