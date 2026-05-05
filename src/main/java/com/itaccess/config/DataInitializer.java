package com.itaccess.config;

import com.itaccess.entity.User;
import com.itaccess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@itaccess.local")
                    .hashedPassword(passwordEncoder.encode("admin123"))
                    .role("admin")
                    .isActive(true)
                    .build();
            
            userRepository.save(admin);
            log.info("===== ADMIN USER CREATED =====");
            log.info("Username: admin");
            log.info("Password: admin123");
            log.info("==============================");
        }
    }
}
