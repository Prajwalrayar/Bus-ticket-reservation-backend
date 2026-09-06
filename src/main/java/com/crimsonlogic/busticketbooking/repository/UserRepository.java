package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByMobileNumber(String mobileNumber);

    Optional<User> findByUserEmail(String userEmail);

    Optional<User> findByUserEmailIgnoreCase(String userEmail);

    boolean existsByUserEmailIgnoreCase(String userEmail);

    boolean existsByMobileNumber(String mobileNumber);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) > 0 FROM User u JOIN u.userRoles ur WHERE u.mobileNumber = :mobileNumber AND ur.roleName = 'PASSENGER'")
    boolean existsByMobileNumberAndRolePassenger(@org.springframework.data.repository.query.Param("mobileNumber") String mobileNumber);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) > 0 FROM User u JOIN u.userRoles ur WHERE u.mobileNumber = :mobileNumber AND ur.roleName != 'PASSENGER'")
    boolean existsByMobileNumberAndRoleStaff(@org.springframework.data.repository.query.Param("mobileNumber") String mobileNumber);

    long countByIsActiveTrue();

    List<User> findByUserEmailContainingIgnoreCaseOrUserNameContainingIgnoreCase(String email, String userName);
}
