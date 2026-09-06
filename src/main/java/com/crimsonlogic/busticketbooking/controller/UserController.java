package com.crimsonlogic.busticketbooking.controller;

import com.crimsonlogic.busticketbooking.dto.*;
import com.crimsonlogic.busticketbooking.entity.User;
import com.crimsonlogic.busticketbooking.entity.UserRole;
import com.crimsonlogic.busticketbooking.exception.ResourceNotFoundException;
import com.crimsonlogic.busticketbooking.repository.UserRepository;
import com.crimsonlogic.busticketbooking.repository.UserRoleRepository;
import com.crimsonlogic.busticketbooking.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    private static final Set<String> VALID_ROLES = Set.of(
            "ADMIN", "BUS_OPERATOR", "SUPPORT_AGENT"
    );


    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getMyProfile() {

        return ResponseEntity.ok(ApiResponse.success(userService.getMyProfile()));
    }

    @GetMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers()));
    }


    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> updateMyProfile(@Valid @RequestBody UserUpdateRequest request) {

        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", userService.updateMyProfile(request)));
    }


    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        
        userService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }


    // =========================================================
// GET USER ROLES
// =========================================================

    @GetMapping("/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserRoleDTO>>> getUserRoles(@PathVariable String userId) {
        List<UserRole> userRoles = userRoleRepository.findByUser_UserId(userId);
        List<UserRoleDTO> userRoleDTOs = userRoles.stream()
                .map(this::convertToUserRoleDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(userRoleDTOs));
    }


// =========================================================
// ASSIGN ROLE TO USER
// =========================================================

    @PostMapping("/{userId}/roles/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserRoleDTO>> assignRole(@PathVariable String userId, @PathVariable String roleName) {

        if (!VALID_ROLES.contains(roleName)) {
            throw new IllegalArgumentException("Invalid role: " + roleName + ". Valid roles are: " + VALID_ROLES);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        UserRole userRole = new UserRole();
        userRole.setRoleName(roleName);
        userRole.setUser(user);
        UserRole savedUserRole = userRoleRepository.save(userRole);

        UserRoleDTO userRoleDTO = convertToUserRoleDTO(savedUserRole);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Role assigned successfully", userRoleDTO));
    }


// =========================================================
// REMOVE ROLE FROM USER
// =========================================================

    @DeleteMapping("/{userId}/roles/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeRole(@PathVariable String userId, @PathVariable String roleName) {

        if (!VALID_ROLES.contains(roleName)) {
            throw new IllegalArgumentException("Invalid role: " + roleName + ". Valid roles are: " + VALID_ROLES);
        }

        UserRole userRole = userRoleRepository.findByUser_UserIdAndRoleName(userId, roleName)
                .orElseThrow(() -> new ResourceNotFoundException("UserRole not found for user: " + userId + " and role: " + roleName));

        userRoleRepository.delete(userRole);

        return ResponseEntity.ok(ApiResponse.success("Role removed successfully", null));
    }


    // =========================================================
    // GET USER ROLES BY EMAIL
    // =========================================================

    @GetMapping("/by-email/{email}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserRoleDTO>>> getUserRolesByEmail(@PathVariable String email) {
        User user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email: " + email));

        List<UserRole> userRoles = userRoleRepository.findByUser_UserId(user.getUserId());
        List<UserRoleDTO> userRoleDTOs = userRoles.stream()
                .map(this::convertToUserRoleDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(userRoleDTOs));
    }


    // =========================================================
    // ASSIGN ROLE TO USER BY EMAIL
    // =========================================================

    @PostMapping("/by-email/{email}/roles/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserRoleDTO>> assignRoleByEmail(@PathVariable String email, @PathVariable String roleName) {

        if (!VALID_ROLES.contains(roleName)) {
            throw new IllegalArgumentException("Invalid role: " + roleName + ". Valid roles are: " + VALID_ROLES);
        }

        User user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email: " + email));

        UserRole userRole = new UserRole();
        userRole.setRoleName(roleName);
        userRole.setUser(user);
        UserRole savedUserRole = userRoleRepository.save(userRole);

        UserRoleDTO userRoleDTO = convertToUserRoleDTO(savedUserRole);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Role assigned successfully", userRoleDTO));
    }


    // =========================================================
    // REMOVE ROLE FROM USER BY EMAIL
    // =========================================================

    @DeleteMapping("/by-email/{email}/roles/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeRoleByEmail(@PathVariable String email, @PathVariable String roleName) {

        if (!VALID_ROLES.contains(roleName)) {
            throw new IllegalArgumentException("Invalid role: " + roleName + ". Valid roles are: " + VALID_ROLES);
        }

        User user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email: " + email));

        UserRole userRole = userRoleRepository.findByUser_UserIdAndRoleName(user.getUserId(), roleName)
                .orElseThrow(() -> new ResourceNotFoundException("UserRole not found for user email: " + email + " and role: " + roleName));

        userRoleRepository.delete(userRole);

        return ResponseEntity.ok(ApiResponse.success("Role removed successfully", null));
    }

    private UserRoleDTO convertToUserRoleDTO(UserRole userRole) {
        return new UserRoleDTO(
                userRole.getUserRoleId(),
                userRole.getUser().getUserId(),
                userRole.getRoleName()
        );
    }
}
