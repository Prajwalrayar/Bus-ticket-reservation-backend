package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    @Query("SELECT l FROM Location l WHERE l.isActive = true AND (LOWER(l.name) LIKE LOWER(CONCAT(:query, '%')) OR l.locationId IN (SELECT la.location.locationId FROM LocationAlias la WHERE la.isActive = true AND LOWER(la.alias) LIKE LOWER(CONCAT(:query, '%'))))")
    List<Location> findByQuery(@Param("query") String query);

}
