package com.crimsonlogic.busticketbooking.config;

import com.crimsonlogic.busticketbooking.entity.User;
import com.crimsonlogic.busticketbooking.entity.UserRole;
import com.crimsonlogic.busticketbooking.entity.UserSecurity;
import com.crimsonlogic.busticketbooking.entity.Wallet;
import com.crimsonlogic.busticketbooking.repository.UserRepository;
import com.crimsonlogic.busticketbooking.repository.UserSecurityRepository;
import com.crimsonlogic.busticketbooking.repository.WalletRepository;
import com.crimsonlogic.busticketbooking.util.EntityIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Initializes default admin data on application startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserSecurityRepository userSecurityRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityIdGenerator entityIdGenerator;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        String adminEmail = "admin@busticket.com";
        String defaultMobile = "0000000000";

        try {
            jdbcTemplate.execute("ALTER TABLE users DROP INDEX idx_users_mobile_number");
            log.info("Dropped idx_users_mobile_number index");
        } catch (Exception e) {
            log.info("Index idx_users_mobile_number not found or already dropped");
        }

        try {
            java.util.List<String> constraints = jdbcTemplate.queryForList(
                "SELECT CONSTRAINT_NAME FROM information_schema.key_column_usage WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'mobile_number' AND CONSTRAINT_NAME != 'PRIMARY'",
                String.class
            );
            for (String constraint : constraints) {
                jdbcTemplate.execute("ALTER TABLE users DROP INDEX " + constraint);
                log.info("Dropped unique constraint/index: " + constraint);
            }
        } catch (Exception e) {
            log.info("Could not drop dynamic unique constraints for mobile_number", e);
        }

        if (!userRepository.existsByUserEmailIgnoreCase(adminEmail)) {
            User admin = new User();
            admin.setUserName("System Admin");
            admin.setFullName("System Admin");
            admin.setUserEmail(adminEmail);
            admin.setMobileNumber(defaultMobile);
            admin.setUserPassword(passwordEncoder.encode("Admin@123"));
            admin.setIsActive(true);
            admin.setIsVerified(true);

            String rolePrefix = EntityIdGenerator.userPrefixForRole("ADMIN");
            admin.setUserId(entityIdGenerator.generate(rolePrefix, userRepository::existsById));

            UserRole adminRole = new UserRole();
            adminRole.setRoleName("ADMIN");
            admin.addUserRole(adminRole);

            admin = userRepository.save(admin);

            UserSecurity userSecurity = new UserSecurity();
            userSecurity.setUser(admin);
            userSecurity.setLoginAttempts(0);
            userSecurityRepository.save(userSecurity);

            Wallet wallet = new Wallet();
            wallet.setUser(admin);
            wallet.setBalance(BigDecimal.ZERO);
            walletRepository.save(wallet);

            log.info("Default admin initialized.");
        } else {
            log.info("Default admin already exists.");
        }
    }
}
