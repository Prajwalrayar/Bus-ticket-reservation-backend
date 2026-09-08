package com.crimsonlogic.busticketbooking.service;

import com.crimsonlogic.busticketbooking.dto.AdminLocationDTO;
import com.crimsonlogic.busticketbooking.dto.LocationDTO;

import java.util.List;

public interface LocationService {
    List<LocationDTO> getSuggestions(String query);
    List<String> getAllNamesForLocationId(Long locationId);

    // Admin Methods
    List<AdminLocationDTO> getAllAdminLocations();
    AdminLocationDTO createLocation(com.crimsonlogic.busticketbooking.dto.LocationCreateRequest request);
    AdminLocationDTO updateLocationName(Long locationId, com.crimsonlogic.busticketbooking.dto.LocationCreateRequest request);
    AdminLocationDTO updateLocationStatus(Long locationId, boolean isActive);

    List<com.crimsonlogic.busticketbooking.dto.AdminLocationAliasDTO> getAliasesForLocation(Long locationId);
    com.crimsonlogic.busticketbooking.dto.AdminLocationAliasDTO createAlias(Long locationId, com.crimsonlogic.busticketbooking.dto.LocationAliasCreateRequest request);
    com.crimsonlogic.busticketbooking.dto.AdminLocationAliasDTO updateAliasName(Long aliasId, com.crimsonlogic.busticketbooking.dto.LocationAliasCreateRequest request);
    com.crimsonlogic.busticketbooking.dto.AdminLocationAliasDTO updateAliasStatus(Long aliasId, boolean isActive);
}
