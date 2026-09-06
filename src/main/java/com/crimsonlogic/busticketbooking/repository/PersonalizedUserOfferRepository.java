package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.PersonalizedUserOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonalizedUserOfferRepository extends JpaRepository<PersonalizedUserOffer, String> {
    List<PersonalizedUserOffer> findByUser_UserId(String userId);
}
