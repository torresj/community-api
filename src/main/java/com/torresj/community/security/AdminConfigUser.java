package com.torresj.community.security;

import com.torresj.community.entities.UserEntity;
import com.torresj.community.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import static com.torresj.community.enums.UserRole.ROLE_SUPERADMIN;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AdminConfigUser {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    @Value("${admin.name:admin}")
    private final String adminName;

    @Value("${admin.password}")
    private final String adminPassword;

    @Bean
    UserEntity createAdminUser() {
        var member = userRepository.findByName(adminName);
        member.ifPresent(userRepository::delete);
        return userRepository.save(
                UserEntity.builder()
                        .role(ROLE_SUPERADMIN)
                        .name(adminName)
                        .password(encoder.encode(adminPassword))
                        .build()
        );
    }
}