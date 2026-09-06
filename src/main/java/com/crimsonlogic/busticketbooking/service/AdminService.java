package com.crimsonlogic.busticketbooking.service;

import com.crimsonlogic.busticketbooking.dto.AdminDashboardDTO;
import com.crimsonlogic.busticketbooking.dto.StaffCreateRequest;
import com.crimsonlogic.busticketbooking.dto.UserDTO;

import java.util.Map;

public interface AdminService {

    AdminDashboardDTO getDashboardKPIs();
    
    UserDTO createStaff(StaffCreateRequest request);
}
