package com.crimsonlogic.busticketbooking.service;

import com.crimsonlogic.busticketbooking.dto.*;

public interface AuthService {

    void register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(String userId, ChangePasswordRequest request);
}
