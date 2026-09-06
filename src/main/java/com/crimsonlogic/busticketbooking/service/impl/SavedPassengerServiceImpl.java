package com.crimsonlogic.busticketbooking.service.impl;

import com.crimsonlogic.busticketbooking.dto.SavedPassengerDTO;
import com.crimsonlogic.busticketbooking.dto.SavedPassengerRequest;
import com.crimsonlogic.busticketbooking.entity.SavedPassenger;
import com.crimsonlogic.busticketbooking.entity.User;
import com.crimsonlogic.busticketbooking.repository.SavedPassengerRepository;
import com.crimsonlogic.busticketbooking.repository.UserRepository;
import com.crimsonlogic.busticketbooking.service.SavedPassengerService;
import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SavedPassengerServiceImpl
        implements SavedPassengerService {

    private final SavedPassengerRepository savedPassengerRepository;
    private final UserRepository userRepository;
    private final EntityIdGenerator entityIdGenerator;


    // =========================================================
    // GET MY SAVED PASSENGERS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<SavedPassengerDTO> getMySavedPassengers() {

        User user = getAuthenticatedUser();

        return savedPassengerRepository
                .findByUser_UserIdAndIsActiveTrueOrderByPassengerNameAsc(
                        user.getUserId()
                )
                .stream()
                .map(this::convertToDTO)
                .toList();
    }


    // =========================================================
    // GET ONE SAVED PASSENGER
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public SavedPassengerDTO getMySavedPassenger(
            String savedPassengerId) {

        User user = getAuthenticatedUser();

        SavedPassenger savedPassenger =
                findPassengerForUser(
                        savedPassengerId,
                        user.getUserId()
                );

        return convertToDTO(savedPassenger);
    }


    // =========================================================
    // CREATE SAVED PASSENGER
    // =========================================================

    @Override
    public SavedPassengerDTO createSavedPassenger(
            SavedPassengerRequest request) {

        User user = getAuthenticatedUser();

        /*
         * Prevent duplicate saved passenger records
         * for the same user.
         */
        if (savedPassengerRepository
                .existsByPassengerNameAndContactNumberAndUser_UserId(
                        request.getPassengerName(),
                        request.getContactNumber(),
                        user.getUserId()
                )) {

            throw new IllegalArgumentException(
                    "This passenger is already saved"
            );
        }

        SavedPassenger savedPassenger =
                new SavedPassenger();

        savedPassenger.setSavedPassengerId(
                EntityIdGenerator.generateStatic(EntityIdGenerator.PREFIX_SAVED_PASSENGER)
        );

        savedPassenger.setPassengerName(
                request.getPassengerName()
        );

        savedPassenger.setAge(
                request.getAge()
        );

        savedPassenger.setGender(
                request.getGender()
        );

        savedPassenger.setIdType(
                request.getIdType()
        );

        savedPassenger.setIdNumber(
                request.getIdNumber()
        );

        savedPassenger.setContactNumber(
                request.getContactNumber()
        );

        savedPassenger.setIsActive(true);

        savedPassenger.setUser(user);

        SavedPassenger saved =
                savedPassengerRepository.save(
                        savedPassenger
                );

        return convertToDTO(saved);
    }


    // =========================================================
    // UPDATE SAVED PASSENGER
    // =========================================================

    @Override
    public SavedPassengerDTO updateSavedPassenger(
            String savedPassengerId,
            SavedPassengerRequest request) {

        User user = getAuthenticatedUser();

        SavedPassenger savedPassenger =
                findPassengerForUser(
                        savedPassengerId,
                        user.getUserId()
                );

        /*
         * Do not update an inactive saved passenger.
         */
        if (!Boolean.TRUE.equals(
                savedPassenger.getIsActive())) {

            throw new IllegalArgumentException(
                    "Saved passenger is inactive"
            );
        }

        savedPassenger.setPassengerName(
                request.getPassengerName()
        );

        savedPassenger.setAge(
                request.getAge()
        );

        savedPassenger.setGender(
                request.getGender()
        );

        savedPassenger.setIdType(
                request.getIdType()
        );

        savedPassenger.setIdNumber(
                request.getIdNumber()
        );

        savedPassenger.setContactNumber(
                request.getContactNumber()
        );

        SavedPassenger updated =
                savedPassengerRepository.save(
                        savedPassenger
                );

        return convertToDTO(updated);
    }


    // =========================================================
    // DEACTIVATE SAVED PASSENGER
    // =========================================================

    @Override
    public void deactivateSavedPassenger(
            String savedPassengerId) {

        User user = getAuthenticatedUser();

        SavedPassenger savedPassenger =
                findPassengerForUser(
                        savedPassengerId,
                        user.getUserId()
                );

        savedPassenger.setIsActive(false);

        savedPassengerRepository.save(
                savedPassenger
        );
    }


    // =========================================================
    // FIND PASSENGER BELONGING TO USER
    // =========================================================

    private SavedPassenger findPassengerForUser(
            String savedPassengerId,
            String userId) {

        return savedPassengerRepository
                .findBySavedPassengerIdAndUser_UserId(
                        savedPassengerId,
                        userId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Saved passenger not found"
                        )
                );
    }


    // =========================================================
    // GET AUTHENTICATED USER
    // =========================================================

    private User getAuthenticatedUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "User is not authenticated"
            );
        }

        String email = authentication.getName();

        return userRepository
                .findByUserEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user not found"
                        )
                );
    }


    // =========================================================
    // ENTITY â†’ DTO
    // =========================================================

    private SavedPassengerDTO convertToDTO(
            SavedPassenger savedPassenger) {

        SavedPassengerDTO dto =
                new SavedPassengerDTO();

        dto.setSavedPassengerId(
                savedPassenger.getSavedPassengerId()
        );

        dto.setPassengerName(
                savedPassenger.getPassengerName()
        );

        dto.setAge(
                savedPassenger.getAge()
        );

        dto.setGender(
                savedPassenger.getGender()
        );

        dto.setIdType(
                savedPassenger.getIdType()
        );

        dto.setIdNumber(
                savedPassenger.getIdNumber()
        );

        dto.setContactNumber(
                savedPassenger.getContactNumber()
        );

        dto.setIsActive(
                savedPassenger.getIsActive()
        );

        dto.setCreatedAt(
                savedPassenger.getCreatedAt()
        );

        dto.setUpdatedAt(
                savedPassenger.getUpdatedAt()
        );

        return dto;
    }
}