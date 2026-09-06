package com.crimsonlogic.busticketbooking.service.impl;

import com.crimsonlogic.busticketbooking.dto.AdminDashboardDTO;
import com.crimsonlogic.busticketbooking.repository.BookingRepository;
import com.crimsonlogic.busticketbooking.repository.BusRepository;
import com.crimsonlogic.busticketbooking.repository.UserRepository;
import com.crimsonlogic.busticketbooking.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final BusRepository busRepository;
    private final BookingRepository bookingRepository;
    private final com.crimsonlogic.busticketbooking.repository.OperatorRepository operatorRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    public AdminDashboardDTO getDashboardKPIs() {

        AdminDashboardDTO dashboard =
                new AdminDashboardDTO();

        dashboard.setTotalUsers(
                userRepository.count()
        );

        dashboard.setActiveUsers(
                userRepository.countByIsActiveTrue()
        );

        dashboard.setTotalBuses(
                busRepository.count()
        );

        dashboard.setActiveBuses(
                busRepository.countByIsActiveTrue()
        );

        dashboard.setTotalBookings(
                bookingRepository.count()
        );

        dashboard.setConfirmedBookings(
                bookingRepository.countByBookingStatus(
                        com.crimsonlogic.busticketbooking.enums.BookingStatus.CONFIRMED
                )
        );

        dashboard.setCancelledBookings(
                bookingRepository.countByBookingStatus(
                        com.crimsonlogic.busticketbooking.enums.BookingStatus.CANCELLED
                )
        );

        return dashboard;
    }

    @Override
    @Transactional
    public com.crimsonlogic.busticketbooking.dto.UserDTO createStaff(com.crimsonlogic.busticketbooking.dto.StaffCreateRequest request) {
        
        if (userRepository.existsByUserEmailIgnoreCase(request.getEmail())) {
            throw new com.crimsonlogic.busticketbooking.exception.BusinessException("A user with this email already exists.", "USER_EXISTS", org.springframework.http.HttpStatus.CONFLICT);
        }

        if (userRepository.existsByMobileNumberAndRoleStaff(request.getMobileNumber())) {
            throw new com.crimsonlogic.busticketbooking.exception.BusinessException("A user with this phone number already exists.", "USER_EXISTS", org.springframework.http.HttpStatus.CONFLICT);
        }

        if (!"BUS_OPERATOR".equals(request.getRoleName()) && !"SUPPORT_AGENT".equals(request.getRoleName())) {
            throw new IllegalArgumentException("Invalid role. Only BUS_OPERATOR or SUPPORT_AGENT are allowed for staff.");
        }

        com.crimsonlogic.busticketbooking.entity.Operator operator = operatorRepository.findByCompanyNameIgnoreCase(request.getCompanyName())
                .orElseThrow(() -> new com.crimsonlogic.busticketbooking.exception.ResourceNotFoundException("Operator not found with name: " + request.getCompanyName()));

        com.crimsonlogic.busticketbooking.entity.User user = new com.crimsonlogic.busticketbooking.entity.User();
        user.setUserName(request.getFullName());
        user.setUserEmail(request.getEmail());
        user.setMobileNumber(request.getMobileNumber());
        user.setUserPassword(passwordEncoder.encode(request.getPassword()));
        user.setIsActive(true);
        user.setIsVerified(true);
        user.setIsTemporaryPassword(true);
        user.setOperator(operator);

        com.crimsonlogic.busticketbooking.entity.UserRole userRole = new com.crimsonlogic.busticketbooking.entity.UserRole();
        userRole.setRoleName(request.getRoleName());
        user.addUserRole(userRole);

        user = userRepository.save(user);

        return new com.crimsonlogic.busticketbooking.dto.UserDTO(
                user.getUserId(),
                user.getUserName(),
                user.getUserEmail(),
                user.getMobileNumber(),
                user.getIsActive(),
                user.getIsVerified(),
                java.util.List.of(request.getRoleName()),
                operator.getCompanyName(),
                user.getCreatedAt(),
                user.getIsTemporaryPassword()
        );
    }
}