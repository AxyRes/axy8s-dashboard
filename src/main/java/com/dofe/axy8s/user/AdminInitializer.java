package com.dofe.axy8s.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(UserRepository userRepository,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        boolean hasSuperAdmin = userRepository.findAll().stream()
                .anyMatch(u -> u.getRole() == Role.SUPER_ADMIN);

        if (hasSuperAdmin) {
            log.info("AXY8S SUPER_ADMIN user already exists, skip auto-creation.");
            return;
        }

        String username = "admin";
        String rawPassword = "123456"; // DEV/LAB ONLY - sau này đổi sang random 12 ký tự

        UserEntity superAdmin = UserEntity.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(Role.SUPER_ADMIN)
                .allowedNamespaces("*") // SUPER_ADMIN ignore rule này, full cluster
                .active(true)
                .systemUser(true)
                .build();

        userRepository.save(superAdmin);

        log.warn("======================================================");
        log.warn(" AXY8S INITIAL SUPER_ADMIN USER CREATED ");
        log.warn("   username: {}", username);
        log.warn("   password: {}", rawPassword);
        log.warn("   NOTE: This is for LAB/DEBUG only. Change it later.");
        log.warn("======================================================");
    }
}
