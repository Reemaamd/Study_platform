package com.study.study_platform.config;

import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class PasswordResetRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner resetPasswordRunner() {

        return args -> {

            String username = "test";
            String newPassword = "123456789";

            Utilisateur user = userRepository
                    .findByUsername(username)
                    .orElse(null);

            if (user == null) {
                System.out.println("❌ User not found");
                return;
            }

            user.setPassword(passwordEncoder.encode(newPassword));

            userRepository.save(user);

            System.out.println("✅ Password updated");
            System.out.println("Username: " + username);
            System.out.println("New password: " + newPassword);
        };
    }
}