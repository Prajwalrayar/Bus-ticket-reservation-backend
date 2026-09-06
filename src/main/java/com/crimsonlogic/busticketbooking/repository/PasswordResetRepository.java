package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetRepository extends JpaRepository<PasswordReset, String> {
    Optional<PasswordReset> findByResetPasswordToken(String resetPasswordToken);
    Optional<PasswordReset> findByUserUserId(String userId);
}
