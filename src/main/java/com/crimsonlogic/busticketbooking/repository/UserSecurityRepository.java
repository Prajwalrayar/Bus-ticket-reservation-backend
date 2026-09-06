package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.UserSecurity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSecurityRepository extends JpaRepository<UserSecurity, String> {
    Optional<UserSecurity> findByUserUserId(String userId);
}
