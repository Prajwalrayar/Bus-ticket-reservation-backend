package com.crimsonlogic.busticketbooking.service.impl;

import com.crimsonlogic.busticketbooking.dto.LocationDTO;
import com.crimsonlogic.busticketbooking.entity.Location;
import com.crimsonlogic.busticketbooking.entity.LocationAlias;
import com.crimsonlogic.busticketbooking.repository.LocationAliasRepository;
import com.crimsonlogic.busticketbooking.repository.LocationRepository;
import com.crimsonlogic.busticketbooking.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final LocationAliasRepository locationAliasRepository;

    @Override
    public List<LocationDTO> getSuggestions(String query) {
        if (query == null || query.trim().length() < 1) {
            return new ArrayList<>();
        }

        String normalizedQuery = query.trim().toLowerCase();
        List<Location> matchingLocations = locationRepository.findByQuery(normalizedQuery);

        return matchingLocations.stream().map(location -> {
            String displayAlias = location.getName();
            
            // Find if any alias matches the query better to display it
            List<LocationAlias> aliases = locationAliasRepository.findByLocation_LocationIdAndIsActiveTrue(location.getLocationId());
            for (LocationAlias alias : aliases) {
                if (alias.getAlias().toLowerCase().startsWith(normalizedQuery)) {
                    displayAlias = alias.getAlias();
                    break;
                }
            }

            return new LocationDTO(location.getLocationId(), location.getName(), displayAlias);
        }).limit(10).collect(Collectors.toList());
    }

    @Override
    public List<String> getAllNamesForLocationId(Long locationId) {
        Location location = locationRepository.findById(locationId).orElseThrow(() -> new IllegalArgumentException("Location not found"));
        List<String> names = new ArrayList<>();
        names.add(location.getName());

        locationAliasRepository.findByLocation_LocationIdAndIsActiveTrue(locationId)
                .forEach(alias -> names.add(alias.getAlias()));

        return names;
    }
}
