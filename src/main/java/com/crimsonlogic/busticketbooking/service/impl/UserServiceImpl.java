package com.crimsonlogic.busticketbooking.service.impl;

import com.crimsonlogic.busticketbooking.dto.ChangePasswordRequest;
import com.crimsonlogic.busticketbooking.dto.UserDTO;
import com.crimsonlogic.busticketbooking.dto.UserUpdateRequest;
import com.crimsonlogic.busticketbooking.entity.User;
import com.crimsonlogic.busticketbooking.entity.UserRole;
import com.crimsonlogic.busticketbooking.repository.UserRepository;
import com.crimsonlogic.busticketbooking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserById(String userId) {

        User currentUser = getCurrentAuthenticatedUser();
        boolean isAdmin = currentUser.getUserRoles().stream()
                .map(UserRole::getRoleName)
                .anyMatch("ADMIN"::equals);

        if (!isAdmin && !currentUser.getUserId().equals(userId)) {
            throw new IllegalStateException(
                    "Access denied: you can only view your own profile"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        return convertToDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserByEmail(String email) {

        User user = userRepository
                .findByUserEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User with email '"
                                        + email
                                        + "' not found"
                        )
                );

        return convertToDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public void deactivateUser(String userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );
        if (user.getUserRoles().stream().anyMatch(role -> role.getRoleName().equals("PASSENGER"))) {
            throw new IllegalArgumentException("Cannot deactivate a passenger account.");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalArgumentException(
                    "User is already inactive"
            );
        }

        user.setIsActive(false);

        userRepository.save(user);
    }

    @Override
    public void activateUser(String userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );
        if (user.getUserRoles().stream().anyMatch(role -> role.getRoleName().equals("PASSENGER"))) {
            throw new IllegalArgumentException("Cannot activate a passenger account.");
        }

        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalArgumentException(
                    "User is already active"
            );
        }

        user.setIsActive(true);

        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getMyProfile() {

        User user = getCurrentAuthenticatedUser();

        return convertToDTO(user);
    }

    @Override
    public UserDTO updateMyProfile(
            UserUpdateRequest request) {

        User user = getCurrentAuthenticatedUser();

        if (request.getUserName() != null) {

            String userName =
                    request.getUserName().trim();

            if (userName.isBlank()) {
                throw new IllegalArgumentException(
                        "User name cannot be blank"
                );
            }

            user.setUserName(userName);
        }

        if (request.getMobileNumber() != null) {

            String mobileNumber =
                    request.getMobileNumber().trim();

            if (!mobileNumber.equals(user.getMobileNumber())) {
                boolean isPassenger = user.getUserRoles().stream()
                        .anyMatch(r -> "PASSENGER".equals(r.getRoleName()));
                
                boolean exists = isPassenger 
                        ? userRepository.existsByMobileNumberAndRolePassenger(mobileNumber)
                        : userRepository.existsByMobileNumberAndRoleStaff(mobileNumber);

                if (exists) {
                    throw new com.crimsonlogic.busticketbooking.exception.BusinessException(
                            "A user with this phone number already exists.", "USER_EXISTS", org.springframework.http.HttpStatus.CONFLICT
                    );
                }
            }

            user.setMobileNumber(mobileNumber);
        }

        User updatedUser =
                userRepository.save(user);

        return convertToDTO(updatedUser);
    }

    @Override
    public void changePassword(
            ChangePasswordRequest request) {

        User user = getCurrentAuthenticatedUser();

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getUserPassword())) {

            throw new IllegalArgumentException(
                    "Current password is incorrect"
            );
        }

        if (!request.getNewPassword()
                .equals(request.getConfirmPassword())) {

            throw new IllegalArgumentException(
                    "New password and confirmation password do not match"
            );
        }

        if (passwordEncoder.matches(
                request.getNewPassword(),
                user.getUserPassword())) {

            throw new IllegalArgumentException(
                    "New password must be different from current password"
            );
        }

        user.setUserPassword(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        user.setIsTemporaryPassword(false);

        userRepository.save(user);
    }

    @Override
    public void deactivateMyAccount() {

        User user = getCurrentAuthenticatedUser();

        if (!Boolean.TRUE.equals(
                user.getIsActive())) {

            throw new IllegalArgumentException(
                    "Account is already inactive"
            );
        }

        user.setIsActive(false);

        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public String getCurrentUserEmail() {

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

        return authentication.getName();
    }

    @Override
    @Transactional(readOnly = true)
    public User getCurrentAuthenticatedUser() {

        String userEmail = getCurrentUserEmail();

        return userRepository
                .findByUserEmail(userEmail)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user not found"
                        )
                );
    }

    private UserDTO convertToDTO(User user) {

        UserDTO dto = new UserDTO();

        dto.setUserId(
                user.getUserId()
        );

        dto.setUserName(
                user.getUserName()
        );

        dto.setUserEmail(
                user.getUserEmail()
        );

        dto.setMobileNumber(
                user.getMobileNumber()
        );

        dto.setIsActive(
                user.getIsActive()
        );

        dto.setIsVerified(
                user.getIsVerified()
        );

        dto.setRoleNames(
                user.getUserRoles().stream()
                        .map(UserRole::getRoleName)
                        .collect(java.util.stream.Collectors.toList())
        );

        if (user.getOperator() != null) {
            dto.setCompanyName(user.getOperator().getCompanyName());
        }

        dto.setCreatedAt(user.getCreatedAt());
        dto.setIsTemporaryPassword(user.getIsTemporaryPassword());

        return dto;
    }

    @Override
    @Transactional
    public UserDTO createSupportAgentForOperator(com.crimsonlogic.busticketbooking.dto.SupportAgentCreateRequest request, String busOperatorEmail) {

        User operatorUser = userRepository.findByUserEmail(busOperatorEmail)
                .orElseThrow(() -> new com.crimsonlogic.busticketbooking.exception.ResourceNotFoundException("User with email: " + busOperatorEmail));

        com.crimsonlogic.busticketbooking.entity.Operator operator = operatorUser.getOperator();
        if (operator == null) {
            throw new IllegalStateException("Current user is not associated with an operator");
        }

        if (userRepository.existsByUserEmailIgnoreCase(request.getEmail())) {
            throw new com.crimsonlogic.busticketbooking.exception.BusinessException("A user with this email already exists.", "USER_EXISTS", org.springframework.http.HttpStatus.CONFLICT);
        }

        if (userRepository.existsByMobileNumberAndRoleStaff(request.getMobileNumber())) {
            throw new com.crimsonlogic.busticketbooking.exception.BusinessException("A user with this phone number already exists.", "USER_EXISTS", org.springframework.http.HttpStatus.CONFLICT);
        }

        User newAgent = new User();
        newAgent.setUserName(request.getFullName());
        newAgent.setUserEmail(request.getEmail());
        newAgent.setMobileNumber(request.getMobileNumber());
        newAgent.setUserPassword(passwordEncoder.encode(request.getPassword()));
        newAgent.setIsActive(true);
        newAgent.setIsVerified(true);
        newAgent.setIsTemporaryPassword(true);
        newAgent.setOperator(operator);

        UserRole role = new UserRole();
        role.setRoleName("SUPPORT_AGENT");
        newAgent.addUserRole(role);

        newAgent = userRepository.save(newAgent);

        return convertToDTO(newAgent);
    }
}
