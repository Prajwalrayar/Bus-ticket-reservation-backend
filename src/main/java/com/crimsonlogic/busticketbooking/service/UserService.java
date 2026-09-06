package com.crimsonlogic.busticketbooking.service;

import com.crimsonlogic.busticketbooking.dto.ChangePasswordRequest;
import com.crimsonlogic.busticketbooking.dto.UserDTO;
import com.crimsonlogic.busticketbooking.dto.UserUpdateRequest;
import com.crimsonlogic.busticketbooking.entity.User;

import java.util.List;

public interface UserService {

    UserDTO getUserById(String userId);

    UserDTO getUserByEmail(String email);

    List<UserDTO> getAllUsers();

    void deactivateUser(String userId);

    void activateUser(String userId);

    UserDTO getMyProfile();

    UserDTO updateMyProfile(UserUpdateRequest request);

    void changePassword(ChangePasswordRequest request);

    void deactivateMyAccount();

    String getCurrentUserEmail();

    User getCurrentAuthenticatedUser();

    UserDTO createSupportAgentForOperator(com.crimsonlogic.busticketbooking.dto.SupportAgentCreateRequest request, String busOperatorEmail);
}
