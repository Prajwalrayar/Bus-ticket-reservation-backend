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

    @Override
    public List<String> getAllNamesForLocationNameOrAlias(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String q = query.trim().toLowerCase();
        java.util.Optional<Location> locationOpt = locationRepository.findByExactNameOrAlias(q);
        if (locationOpt.isPresent()) {
            return getAllNamesForLocationId(locationOpt.get().getLocationId());
        }
        
        // Semantic meaning / Typo Correction via Levenshtein Distance
        List<Location> allLocations = locationRepository.findAll();
        Location bestMatch = null;
        int bestDistance = Integer.MAX_VALUE;

        for (Location loc : allLocations) {
            if (!loc.getIsActive()) continue;
            
            int dist = calculateLevenshtein(q, loc.getName().toLowerCase());
            if (dist < bestDistance) {
                bestDistance = dist;
                bestMatch = loc;
            }

            List<LocationAlias> aliases = locationAliasRepository.findByLocation_LocationIdAndIsActiveTrue(loc.getLocationId());
            for (LocationAlias alias : aliases) {
                dist = calculateLevenshtein(q, alias.getAlias().toLowerCase());
                if (dist < bestDistance) {
                    bestDistance = dist;
                    bestMatch = loc;
                }
            }
        }

        // Allow up to 3 typos for a match
        if (bestMatch != null && bestDistance <= 3) {
            return getAllNamesForLocationId(bestMatch.getLocationId());
        }

        return java.util.List.of(query.trim()); // Fallback to raw string if not found
    }

    private int calculateLevenshtein(String x, String y) {
        int[][] dp = new int[x.length() + 1][y.length() + 1];
        for (int i = 0; i <= x.length(); i++) {
            for (int j = 0; j <= y.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(dp[i - 1][j - 1] 
                     + (x.charAt(i - 1) == y.charAt(j - 1) ? 0 : 1), 
                     Math.min(dp[i - 1][j] + 1, 
                     dp[i][j - 1] + 1));
                }
            }
        }
        return dp[x.length()][y.length()];
    }

    @Override
    public List<com.crimsonlogic.busticketbooking.dto.AdminLocationDTO> getAllAdminLocations() {
        return locationRepository.findAll().stream()
                .map(loc -> new com.crimsonlogic.busticketbooking.dto.AdminLocationDTO(
                        loc.getLocationId(), loc.getName(), loc.getIsActive(), loc.getCreatedAt(), loc.getUpdatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public com.crimsonlogic.busticketbooking.dto.AdminLocationDTO createLocation(com.crimsonlogic.busticketbooking.dto.LocationCreateRequest request) {
        String normalizedName = request.getName().trim();
        if (locationRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new IllegalArgumentException("Location with this name already exists.");
        }
        Location loc = new Location();
        loc.setName(normalizedName);
        loc.setIsActive(true);
        loc = locationRepository.save(loc);
        return new com.crimsonlogic.busticketbooking.dto.AdminLocationDTO(
                loc.getLocationId(), loc.getName(), loc.getIsActive(), loc.getCreatedAt(), loc.getUpdatedAt());
    }

    @Override
    @Transactional
    public com.crimsonlogic.busticketbooking.dto.AdminLocationDTO updateLocationName(Long locationId, com.crimsonlogic.busticketbooking.dto.LocationCreateRequest request) {
        Location loc = locationRepository.findById(locationId)
                .orElseThrow(() -> new com.crimsonlogic.busticketbooking.exception.ResourceNotFoundException("Location not found"));
        String normalizedName = request.getName().trim();
        if (!loc.getName().equalsIgnoreCase(normalizedName) && locationRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new IllegalArgumentException("Location with this name already exists.");
        }
        loc.setName(normalizedName);
        loc = locationRepository.save(loc);
        return new com.crimsonlogic.busticketbooking.dto.AdminLocationDTO(
                loc.getLocationId(), loc.getName(), loc.getIsActive(), loc.getCreatedAt(), loc.getUpdatedAt());
    }

    @Override
    @Transactional
    public com.crimsonlogic.busticketbooking.dto.AdminLocationDTO updateLocationStatus(Long locationId, boolean isActive) {
        Location loc = locationRepository.findById(locationId)
                .orElseThrow(() -> new com.crimsonlogic.busticketbooking.exception.ResourceNotFoundException("Location not found"));
        loc.setIsActive(isActive);
        loc = locationRepository.save(loc);
        return new com.crimsonlogic.busticketbooking.dto.AdminLocationDTO(
                loc.getLocationId(), loc.getName(), loc.getIsActive(), loc.getCreatedAt(), loc.getUpdatedAt());
    }

    @Override
    public List<com.crimsonlogic.busticketbooking.dto.AdminLocationAliasDTO> getAliasesForLocation(Long locationId) {
        return locationAliasRepository.findByLocation_LocationId(locationId).stream()
                .map(alias -> new com.crimsonlogic.busticketbooking.dto.AdminLocationAliasDTO(
                        alias.getAliasId(), alias.getAlias(), alias.getIsActive(), alias.getCreatedAt(), alias.getUpdatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public com.crimsonlogic.busticketbooking.dto.AdminLocationAliasDTO createAlias(Long locationId, com.crimsonlogic.busticketbooking.dto.LocationAliasCreateRequest request) {
        Location loc = locationRepository.findById(locationId)
                .orElseThrow(() -> new com.crimsonlogic.busticketbooking.exception.ResourceNotFoundException("Location not found"));
        String normalizedAlias = request.getAlias().trim();
        if (locationAliasRepository.existsByAliasIgnoreCase(normalizedAlias)) {
            throw new IllegalArgumentException("Alias with this name already exists.");
        }
        if (loc.getName().equalsIgnoreCase(normalizedAlias)) {
            throw new IllegalArgumentException("Alias cannot be the same as the canonical location name.");
        }
        LocationAlias alias = new LocationAlias();
        alias.setLocation(loc);
        alias.setAlias(normalizedAlias);
        alias.setIsActive(true);
        alias = locationAliasRepository.save(alias);
        return new com.crimsonlogic.busticketbooking.dto.AdminLocationAliasDTO(
                alias.getAliasId(), alias.getAlias(), alias.getIsActive(), alias.getCreatedAt(), alias.getUpdatedAt());
    }

    @Override
    @Transactional
    public com.crimsonlogic.busticketbooking.dto.AdminLocationAliasDTO updateAliasName(Long aliasId, com.crimsonlogic.busticketbooking.dto.LocationAliasCreateRequest request) {
        LocationAlias alias = locationAliasRepository.findById(aliasId)
                .orElseThrow(() -> new com.crimsonlogic.busticketbooking.exception.ResourceNotFoundException("Alias not found"));
        String normalizedAlias = request.getAlias().trim();
        if (!alias.getAlias().equalsIgnoreCase(normalizedAlias) && locationAliasRepository.existsByAliasIgnoreCase(normalizedAlias)) {
            throw new IllegalArgumentException("Alias with this name already exists.");
        }
        if (alias.getLocation().getName().equalsIgnoreCase(normalizedAlias)) {
            throw new IllegalArgumentException("Alias cannot be the same as the canonical location name.");
        }
        alias.setAlias(normalizedAlias);
        alias = locationAliasRepository.save(alias);
        return new com.crimsonlogic.busticketbooking.dto.AdminLocationAliasDTO(
                alias.getAliasId(), alias.getAlias(), alias.getIsActive(), alias.getCreatedAt(), alias.getUpdatedAt());
    }

    @Override
    @Transactional
    public com.crimsonlogic.busticketbooking.dto.AdminLocationAliasDTO updateAliasStatus(Long aliasId, boolean isActive) {
        LocationAlias alias = locationAliasRepository.findById(aliasId)
                .orElseThrow(() -> new com.crimsonlogic.busticketbooking.exception.ResourceNotFoundException("Alias not found"));
        alias.setIsActive(isActive);
        alias = locationAliasRepository.save(alias);
        return new com.crimsonlogic.busticketbooking.dto.AdminLocationAliasDTO(
                alias.getAliasId(), alias.getAlias(), alias.getIsActive(), alias.getCreatedAt(), alias.getUpdatedAt());
    }
}
