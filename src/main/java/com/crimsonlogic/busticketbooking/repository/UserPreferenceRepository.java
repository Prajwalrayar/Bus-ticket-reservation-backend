package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, String> {

    Optional<UserPreference> findByUser_UserId(String userId);

}
