package com.crimsonlogic.busticketbooking.service.impl;

import com.crimsonlogic.busticketbooking.entity.Operator;
import com.crimsonlogic.busticketbooking.repository.OperatorRepository;
import com.crimsonlogic.busticketbooking.repository.UserRepository;
import com.crimsonlogic.busticketbooking.service.OperatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OperatorServiceImpl implements OperatorService {

    private final OperatorRepository operatorRepository;
    private final UserRepository userRepository;

    /**
     * Ensures the currently authenticated BUS_OPERATOR belongs to the given company.
     * ADMINs bypass this check.
     */
    private void authorizeOperatorOwnership(String companyName) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        com.crimsonlogic.busticketbooking.entity.User user = userRepository
                .findByUserEmailIgnoreCase(email)
                .orElseThrow(() -> new AccessDeniedException("User not found"));

        boolean isAdmin = user.getUserRoles().stream()
                .anyMatch(r -> "ADMIN".equals(r.getRoleName()));
        if (isAdmin) return;

        if (user.getOperator() == null
                || !user.getOperator().getCompanyName().equalsIgnoreCase(companyName)) {
            throw new AccessDeniedException("You are not authorized to access this resource.");
        }
    }

    @Override
    public Operator createOperator(Operator operator) {

        if (operatorRepository.existsByCompanyNameIgnoreCase(operator.getCompanyName())) {

            throw new IllegalArgumentException("Operator with company name '"
                            + operator.getCompanyName() + "' already exists");
        }

        if (operatorRepository.existsByContactEmailIgnoreCase(
                operator.getContactEmail())) {

            throw new IllegalArgumentException(
                    "Operator with contact email '"
                            + operator.getContactEmail()
                            + "' already exists"
            );
        }

        /*
         * Newly registered operators require approval.
         */
        operator.setIsApproved(false);

        /*
         * Newly registered operators are active by default.
         */
        operator.setIsActive(true);

        return operatorRepository.save(operator);
    }

    @Override
    @Transactional(readOnly = true)
    public Operator getOperatorByCompanyName(String companyName) {

        return operatorRepository
                .findByCompanyNameIgnoreCase(companyName)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Operator with company name '"
                                        + companyName
                                        + "' not found"
                        )
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Operator> getAllOperators() {

        return operatorRepository.findAll();
    }

    @Override
    public Operator updateOperator(
            String companyName,
            Operator updatedOperator) {

        // ── Ownership check ───────────────────────────────────────
        // BUS_OPERATOR can only update their own company.
        authorizeOperatorOwnership(companyName);
        // ─────────────────────────────────────────────────────────

        Operator existingOperator =
                operatorRepository
                        .findByCompanyNameIgnoreCase(companyName)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Operator with company name '"
                                                + companyName
                                                + "' not found"
                                )
                        );

        /*
         * Check whether the new company name is already
         * used by another operator.
         */
        if (!existingOperator.getCompanyName()
                .equalsIgnoreCase(updatedOperator.getCompanyName())
                && operatorRepository.existsByCompanyNameIgnoreCase(
                updatedOperator.getCompanyName())) {

            throw new IllegalArgumentException(
                    "Another operator with company name '"
                            + updatedOperator.getCompanyName()
                            + "' already exists"
            );
        }

        /*
         * Check whether the new email is already used
         * by another operator.
         */
        if (!existingOperator.getContactEmail()
                .equalsIgnoreCase(updatedOperator.getContactEmail())
                && operatorRepository.existsByContactEmailIgnoreCase(
                updatedOperator.getContactEmail())) {

            throw new IllegalArgumentException(
                    "Another operator with contact email '"
                            + updatedOperator.getContactEmail()
                            + "' already exists"
            );
        }

        /*
         * Update only editable operator information.
         *
         * We do not change:
         * - operatorId
         * - approval status
         * - approvedAt
         * - createdAt
         * - updatedAt
         */
        existingOperator.setCompanyName(
                updatedOperator.getCompanyName()
        );

        existingOperator.setContactEmail(
                updatedOperator.getContactEmail()
        );

        existingOperator.setContactPhone(
                updatedOperator.getContactPhone()
        );

        existingOperator.setLogoUrl(
                updatedOperator.getLogoUrl()
        );

        existingOperator.setWebsiteUrl(
                updatedOperator.getWebsiteUrl()
        );

        return operatorRepository.save(existingOperator);
    }

    @Override
    public void approveOperator(String companyName) {

        Operator operator =
                operatorRepository
                        .findByCompanyNameIgnoreCase(companyName)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Operator with company name '"
                                                + companyName
                                                + "' not found"
                                )
                        );

        if (Boolean.TRUE.equals(operator.getIsApproved())) {
            throw new IllegalArgumentException(
                    "Operator is already approved"
            );
        }

        operator.setIsApproved(true);
        operator.setApprovedAt(java.time.LocalDateTime.now());

        operatorRepository.save(operator);
    }

    @Override
    public void deactivateOperator(String companyName) {

        Operator operator =
                operatorRepository
                        .findByCompanyNameIgnoreCase(companyName)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Operator with company name '"
                                                + companyName
                                                + "' not found"
                                )
                        );

        if (!Boolean.TRUE.equals(operator.getIsActive())) {
            throw new IllegalArgumentException(
                    "Operator is already inactive"
            );
        }

        /*
         * Soft deactivation instead of physical deletion.
         * Historical trips and bookings can still reference
         * this operator.
         */
        operator.setIsActive(false);

        operatorRepository.save(operator);
    }}
