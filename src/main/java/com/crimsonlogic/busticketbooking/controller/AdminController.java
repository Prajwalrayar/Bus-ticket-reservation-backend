package com.crimsonlogic.busticketbooking.controller;

import com.crimsonlogic.busticketbooking.dto.AdminDashboardDTO;
import com.crimsonlogic.busticketbooking.dto.ApiResponse;
import com.crimsonlogic.busticketbooking.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardDTO>>
    getDashboard() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        adminService.getDashboardKPIs()
                )
        );
    }

    @org.springframework.web.bind.annotation.PostMapping("/staff")
    public ResponseEntity<ApiResponse<com.crimsonlogic.busticketbooking.dto.UserDTO>> createStaff(
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.crimsonlogic.busticketbooking.dto.StaffCreateRequest request) {

        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(
                ApiResponse.success("Staff user created successfully", adminService.createStaff(request))
        );
    }
}