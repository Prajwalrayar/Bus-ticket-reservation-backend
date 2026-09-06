package com.crimsonlogic.busticketbooking.service;

import com.crimsonlogic.busticketbooking.dto.AuditLogDTO;

import java.util.List;

public interface AuditLogService {

    void createAuditLog(
            String action,
            String entityName,
            String entityReference,
            String description
    );

    List<AuditLogDTO> getMyAuditLogs();

    List<AuditLogDTO> getAuditLogsByEntity(
            String entityName,
            String entityReference
    );

    List<AuditLogDTO> getAuditLogsByAction(
            String action
    );

    List<AuditLogDTO> getAllAuditLogs();
}