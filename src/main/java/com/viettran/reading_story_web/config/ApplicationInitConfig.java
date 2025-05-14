package com.viettran.reading_story_web.config;

import java.time.Instant;
import java.util.HashSet;

import com.viettran.reading_story_web.repository.jpa.RoleRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.viettran.reading_story_web.entity.mysql.Level;
import com.viettran.reading_story_web.entity.mysql.Role;
import com.viettran.reading_story_web.entity.mysql.User;
import com.viettran.reading_story_web.repository.jpa.LevelRepository;
import com.viettran.reading_story_web.repository.jpa.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    PasswordEncoder passwordEncoder;

    static final String ADMIN_EMAIL = "numberzero0909@gmail.com";
    static final String ADMIN_PASSWORD = "adminweb";

    @Bean
    ApplicationRunner applicationRunner(
            UserRepository userRepository, RoleRepository roleRepository, LevelRepository levelRepository) {
        log.info("Initializing application.....");
        return args -> {
            if (userRepository.findByEmail(ADMIN_EMAIL).isEmpty()) {

                var roles = new HashSet<Role>();

                Role adminRole = roleRepository
                        .findById("ADMIN")
                        .orElseGet(() -> roleRepository.save(Role.builder()
                                .name("ADMIN")
                                .description("Admin role")
                                .build()));

                Role userRole = roleRepository
                        .findById("USER")
                        .orElseGet(() -> roleRepository.save(Role.builder()
                                .name("USER")
                                .description("User role")
                                .build()));

                roles.add(adminRole);
                roles.add(userRole);

                User user = User.builder()
                        .email(ADMIN_EMAIL)
                        .name("admin")
                        .password(passwordEncoder.encode(ADMIN_PASSWORD))
                        .roles(roles)
                        .isVerified(true)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();

                userRepository.save(user);

                Level level = Level.builder().user(user).build();

                levelRepository.save(level);

                log.warn("admin has been created with default password: adminweb, please change it");
            }
        };
    }
}
