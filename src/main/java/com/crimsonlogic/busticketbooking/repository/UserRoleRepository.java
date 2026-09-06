package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.User;
import com.crimsonlogic.busticketbooking.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, String> {

    List<UserRole> findByUser_UserId(String userId);

    List<UserRole> findByUser(User user);

    Optional<UserRole> findByUserAndRoleName(User user, String roleName);

    Optional<UserRole> findByUser_UserIdAndRoleName(String userId, String roleName);

    boolean existsByUser_UserIdAndRoleName(String userId, String roleName);
}
