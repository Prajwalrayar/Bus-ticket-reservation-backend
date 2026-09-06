package com.crimsonlogic.busticketbooking.controller;

import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.entity.Operator;
import com.crimsonlogic.busticketbooking.service.OperatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operators")
@RequiredArgsConstructor
public class OperatorController {

    private final OperatorService operatorService;
    private final com.crimsonlogic.busticketbooking.service.UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Operator>>> getAllOperators() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        operatorService.getAllOperators()
                )
        );
    }

    @GetMapping("/{companyName}")
    public ResponseEntity<ApiResponse<Operator>> getOperatorByCompanyName(
            @PathVariable String companyName) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        operatorService.getOperatorByCompanyName(companyName)
                )
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Operator>> createOperator(
            @RequestBody Operator operator) {

        Operator createdOperator =
                operatorService.createOperator(operator);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdOperator));
    }

    @PutMapping("/{companyName}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BUS_OPERATOR')")
    public ResponseEntity<ApiResponse<Operator>> updateOperator(
            @PathVariable String companyName,
            @RequestBody Operator operator) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        operatorService.updateOperator(
                                companyName,
                                operator
                        )
                )
        );
    }

    @PatchMapping("/{companyName}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> approveOperator(
            @PathVariable String companyName) {

        operatorService.approveOperator(companyName);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Operator approved",
                        null
                )
        );
    }

    @PatchMapping("/{companyName}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateOperator(
            @PathVariable String companyName) {

        operatorService.deactivateOperator(companyName);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Operator deactivated",
                        null
                )
        );
    }

    @PostMapping("/support-agents")
    @PreAuthorize("hasRole('BUS_OPERATOR')")
    public ResponseEntity<ApiResponse<com.crimsonlogic.busticketbooking.dto.UserDTO>> createSupportAgent(
            @jakarta.validation.Valid @RequestBody com.crimsonlogic.busticketbooking.dto.SupportAgentCreateRequest request,
            org.springframework.security.core.Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        "Support Agent created successfully",
                        userService.createSupportAgentForOperator(request, authentication.getName())
                )
        );
    }
}