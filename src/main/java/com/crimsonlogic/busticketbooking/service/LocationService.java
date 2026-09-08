package com.crimsonlogic.busticketbooking.service;

import com.crimsonlogic.busticketbooking.dto.LocationDTO;

import java.util.List;

public interface LocationService {
    List<LocationDTO> getSuggestions(String query);
    List<String> getAllNamesForLocationId(Long locationId);
}
