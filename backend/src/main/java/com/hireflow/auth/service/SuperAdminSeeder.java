package com.hireflow.auth.service;

import java.util.Locale;

import com.hireflow.auth.dto.validation.StrongPasswordValidator;
import com.hireflow.auth.entity.User;
import com.hireflow.auth.repository.UserRepository;
import com.hireflow.security.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Creates the platform's first SUPER_ADMIN at startup, because nobody can register as one.
 * Done here rather than in a SQL migration so the password is BCrypt-hashed by the same encoder
 * as every other password, and never appears in the repository.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class SuperAdminSeeder implements ApplicationRunner {

    private final SuperAdminProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.SUPER_ADMIN)) {
            return;
        }
        if (!StringUtils.hasText(properties.email()) || !StringUtils.hasText(properties.password())) {
            log.warn("No super admin exists. Set SUPER_ADMIN_EMAIL and SUPER_ADMIN_PASSWORD to create one.");
            return;
        }
        if (!new StrongPasswordValidator().isValid(properties.password(), null)) {
            log.error("SUPER_ADMIN_PASSWORD does not meet the password policy; super admin not created.");
            return;
        }

        String email = properties.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            log.error("SUPER_ADMIN_EMAIL {} already belongs to another account; super admin not created.", email);
            return;
        }

        String name = StringUtils.hasText(properties.name()) ? properties.name().trim() : "Platform Admin";
        userRepository.save(User.create(name, email, passwordEncoder.encode(properties.password()),
                Role.SUPER_ADMIN, null));
        log.info("Created super admin {}", email);
    }
}
