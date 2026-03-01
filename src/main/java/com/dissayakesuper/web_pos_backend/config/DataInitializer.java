package com.dissayakesuper.web_pos_backend.config;

import com.dissayakesuper.web_pos_backend.user.entity.User;
import com.dissayakesuper.web_pos_backend.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Seeds the very first Owner/Admin account when the users table is completely
 * empty (i.e. a brand-new deployment with no Flyway seed data).
 *
 * Credentials:  username = admin  |  password = admin123  |  role = Owner
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository        userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           BCryptPasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Only seed when the table is completely empty
        if (userRepository.count() > 0) {
            return;
        }

        User admin = User.builder()
                .username("admin")
                .fullName("System Administrator")
                .email("admin@dissanayake.lk")
                .passwordHash(passwordEncoder.encode("admin123"))
                .role("Owner")
                .active(true)
                .emailNotifications(false)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(admin);

        System.out.println("[DataInitializer] Default admin account created — username: admin / password: admin123");
    }
}
