package com.crimsonlogic.busticketbooking.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.dto.AuditLogDTO;
import com.crimsonlogic.busticketbooking.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;


    @GetMapping("/entity")
    public ResponseEntity<ApiResponse<List<AuditLogDTO>>>
    getAuditLogsByEntity(
            @RequestParam String entityName,
            @RequestParam String entityReference) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        auditLogService.getAuditLogsByEntity(
                                entityName,
                                entityReference
                        )
                )
        );
    }


    @GetMapping("/action/{action}")
    public ResponseEntity<ApiResponse<List<AuditLogDTO>>>
    getAuditLogsByAction(
            @PathVariable String action) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        auditLogService.getAuditLogsByAction(
                                action
                        )
                )
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AuditLogDTO>>>
    getAllAuditLogs() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        auditLogService.getAllAuditLogs()
                )
        );
    }
}