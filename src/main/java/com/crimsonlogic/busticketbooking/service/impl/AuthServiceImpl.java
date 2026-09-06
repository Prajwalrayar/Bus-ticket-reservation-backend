package com.crimsonlogic.busticketbooking.service.impl;

import com.crimsonlogic.busticketbooking.dto.*;
import com.crimsonlogic.busticketbooking.entity.User;
import com.crimsonlogic.busticketbooking.entity.UserRole;
import com.crimsonlogic.busticketbooking.entity.UserSecurity;
import com.crimsonlogic.busticketbooking.entity.Wallet;
import com.crimsonlogic.busticketbooking.entity.PasswordReset;
import com.crimsonlogic.busticketbooking.exception.InvalidCredentialsException;
import com.crimsonlogic.busticketbooking.exception.ResourceNotFoundException;
import com.crimsonlogic.busticketbooking.exception.BusinessException;
import com.crimsonlogic.busticketbooking.repository.UserRepository;
import com.crimsonlogic.busticketbooking.repository.UserSecurityRepository;
import com.crimsonlogic.busticketbooking.repository.PasswordResetRepository;
import com.crimsonlogic.busticketbooking.repository.WalletRepository;
import com.crimsonlogic.busticketbooking.service.AuthService;
import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;
import com.crimsonlogic.busticketbooking.util.JwtUtil;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final WalletRepository walletRepository;
    private final UserSecurityRepository userSecurityRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final EntityIdGenerator entityIdGenerator;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public void register(RegisterRequest request) {
        log.info("Registering user with email: {}", request.getEmail());

        if (userRepository.existsByUserEmailIgnoreCase(request.getEmail())) {
            throw new BusinessException("A user with this email already exists.", "USER_EXISTS", HttpStatus.CONFLICT);
        }

        if (userRepository.existsByMobileNumberAndRolePassenger(request.getMobileNumber())) {
            throw new BusinessException("A user with this phone number already exists.", "USER_EXISTS", HttpStatus.CONFLICT);
        }

        if (request.getConfirmPassword() != null && !request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = new User();
        user.setUserName(request.getFullName());
        user.setUserEmail(request.getEmail());
        user.setMobileNumber(request.getMobileNumber());
        user.setUserPassword(passwordEncoder.encode(request.getPassword()));
        user.setIsActive(true);
        user.setIsVerified(true);

        // Set role-based userId BEFORE save so @PrePersist fallback is not used
        String rolePrefix = EntityIdGenerator.userPrefixForRole("PASSENGER");
        user.setUserId(entityIdGenerator.generate(rolePrefix, userRepository::existsById));

        UserRole userRole = new UserRole();
        userRole.setRoleName("PASSENGER");
        user.addUserRole(userRole);

        user = userRepository.save(user);
        
        UserSecurity userSecurity = new UserSecurity();
        userSecurity.setUser(user);
        userSecurity.setLoginAttempts(0);
        userSecurityRepository.save(userSecurity);
        
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(BigDecimal.ZERO);
        walletRepository.save(wallet);

        log.info("User registered successfully: {}", user.getUserEmail());
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        User user = userRepository.findByUserEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!user.getIsActive()) {
            throw new InvalidCredentialsException("Account is deactivated. Contact support.");
        }

        UserSecurity userSecurity = userSecurityRepository.findByUserUserId(user.getUserId())
                .orElseGet(() -> {
                    UserSecurity us = new UserSecurity();
                    us.setUser(user);
                    us.setLoginAttempts(0);
                    return userSecurityRepository.save(us);
                });

        if (userSecurity.getAccountLockedUntil() != null && userSecurity.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
            throw new InvalidCredentialsException("Account is temporarily locked. Try again later.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            userSecurity.setLoginAttempts(0);
            userSecurity.setLastLoginTime(LocalDateTime.now());
            userSecurity.setAccountLockedUntil(null);
            userSecurityRepository.save(userSecurity);

            List<String> roles = user.getUserRoles().stream()
                    .map(UserRole::getRoleName)
                    .collect(Collectors.toList());

            String token = jwtUtil.generateToken(user.getUserEmail(), user.getUserId(), roles);

            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setTokenType("Bearer");
            response.setUserId(user.getUserId());
            response.setUserName(user.getUserName());
            response.setUserEmail(user.getUserEmail());
            response.setRoles(roles);
            response.setIsTemporaryPassword(user.getIsTemporaryPassword());
            // Set exact expiration corresponding to the JWT
            response.setExpiresAt(LocalDateTime.now().plusNanos(jwtUtil.getExpiration() * 1000000L));

            log.info("User logged in successfully: {}", user.getUserEmail());
            return response;
        } catch (Exception e) {
            userSecurity.setLoginAttempts(userSecurity.getLoginAttempts() + 1);
            if (userSecurity.getLoginAttempts() >= 5) {
                userSecurity.setAccountLockedUntil(LocalDateTime.now().plusMinutes(30));
                log.warn("Account locked for user: {}", user.getUserEmail());
            }
            userSecurityRepository.save(userSecurity);
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("Forgot password request for: {}", request.getEmail());

        User user = userRepository.findByUserEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

        // Password reset token is a security token — intentionally kept as UUID (not a business ID)
        String resetToken = generateSecureResetToken();
        PasswordReset passwordReset = passwordResetRepository.findByUserUserId(user.getUserId())
                .orElse(new PasswordReset());
        passwordReset.setUser(user);
        passwordReset.setResetPasswordToken(resetToken);
        passwordReset.setResetTokenExpiryTime(LocalDateTime.now().plusHours(1));
        passwordResetRepository.save(passwordReset);

        log.info("Password reset token generated for user: {}. Token: {}", user.getUserEmail(), resetToken);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Reset password with token");

        PasswordReset passwordReset = passwordResetRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired reset token"));

        if (passwordReset.getResetTokenExpiryTime().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException("Reset token has expired");
        }

        User user = passwordReset.getUser();
        user.setUserPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        passwordResetRepository.delete(passwordReset);

        log.info("Password reset successfully for user: {}", user.getUserEmail());
    }

    @Override
    public void changePassword(String userId, ChangePasswordRequest request) {
        log.info("Change password for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getUserPassword())) {
            throw new InvalidCredentialsException("Incorrect current password");
        }

        user.setUserPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for userId: {}", userId);
    }

    /**
     * Generate a cryptographically secure reset token.
     * Intentionally uses raw randomness — this is a security token, not a business ID.
     */
    private String generateSecureResetToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
