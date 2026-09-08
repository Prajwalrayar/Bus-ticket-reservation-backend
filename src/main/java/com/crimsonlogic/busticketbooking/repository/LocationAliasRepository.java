package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.LocationAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationAliasRepository extends JpaRepository<LocationAlias, Long> {
    List<LocationAlias> findByLocation_LocationIdAndIsActiveTrue(Long locationId);
    
    List<LocationAlias> findByLocation_LocationId(Long locationId);
    
    boolean existsByAliasIgnoreCase(String alias);
}
