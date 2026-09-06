package com.crimsonlogic.busticketbooking.service.impl;

import com.crimsonlogic.busticketbooking.dto.AuditLogDTO;
import com.crimsonlogic.busticketbooking.entity.AuditLog;
import com.crimsonlogic.busticketbooking.entity.User;
import com.crimsonlogic.busticketbooking.repository.AuditLogRepository;
import com.crimsonlogic.busticketbooking.repository.UserRepository;
import com.crimsonlogic.busticketbooking.service.AuditLogService;
import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AuditLogServiceImpl
        implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final EntityIdGenerator entityIdGenerator;


    // =========================================================
    // CREATE AUDIT LOG
    // =========================================================

    @Override
    public void createAuditLog(
            String action,
            String entityName,
            String entityReference,
            String description) {

        User performedByUser =
                getAuthenticatedUser();

        AuditLog auditLog =
                new AuditLog();

        auditLog.setAuditLogId(
                EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_AUDIT_LOG)
        );

        auditLog.setAction(action);

        auditLog.setEntityName(entityName);

        auditLog.setEntityReference(
                entityReference
        );

        auditLog.setDescription(
                description
        );

        auditLog.setPerformedByUser(
                performedByUser
        );

        auditLogRepository.save(auditLog);
    }


    // =========================================================
    // GET MY AUDIT LOGS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDTO> getMyAuditLogs() {

        User user =
                getAuthenticatedUser();

        return auditLogRepository
                .findByPerformedByUser_UserIdOrderByCreatedAtDesc(
                        user.getUserId()
                )
                .stream()
                .map(this::convertToDTO)
                .toList();
    }


    // =========================================================
    // GET AUDIT LOGS FOR ENTITY
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDTO> getAuditLogsByEntity(
            String entityName,
            String entityReference) {

        return auditLogRepository
                .findByEntityNameAndEntityReferenceOrderByCreatedAtDesc(
                        entityName,
                        entityReference
                )
                .stream()
                .map(this::convertToDTO)
                .toList();
    }


    // =========================================================
    // GET AUDIT LOGS BY ACTION
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDTO> getAuditLogsByAction(
            String action) {

        return auditLogRepository
                .findByActionOrderByCreatedAtDesc(
                        action
                )
                .stream()
                .map(this::convertToDTO)
                .toList();
    }


    // =========================================================
    // GET ALL AUDIT LOGS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDTO> getAllAuditLogs() {

        // Assuming auditLogRepository has a findAll() or similar method returning List<AuditLog>.
        // To sort by CreatedAt descending, we might have to sort it in memory if the repository doesn't provide it, 
        // but typically findAll() is fine, let's sort via stream for safety or assume we can use stream().sorted()
        return auditLogRepository
                .findAll()
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::convertToDTO)
                .toList();
    }


    // =========================================================
    // GET AUTHENTICATED USER
    // =========================================================

    private User getAuthenticatedUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "User is not authenticated"
            );
        }

        String email =
                authentication.getName();

        return userRepository
                .findByUserEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user not found"
                        )
                );
    }


    // =========================================================
    // ENTITY â†’ DTO
    // =========================================================

    private AuditLogDTO convertToDTO(
            AuditLog auditLog) {

        AuditLogDTO dto =
                new AuditLogDTO();

        dto.setAuditLogId(
                auditLog.getAuditLogId()
        );

        dto.setAction(
                auditLog.getAction()
        );

        dto.setEntityName(
                auditLog.getEntityName()
        );

        dto.setEntityReference(
                auditLog.getEntityReference()
        );

        dto.setDescription(
                auditLog.getDescription()
        );

        if (auditLog.getPerformedByUser() != null) {

            dto.setPerformedByUserName(
                    auditLog.getPerformedByUser()
                            .getUserName()
            );
        }

        dto.setCreatedAt(
                auditLog.getCreatedAt()
        );

        return dto;
    }
}