package com.study.study_platform;

import com.study.study_platform.security.JwtUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class TestAuthRunner implements CommandLineRunner {

    private final JwtUtils jwtUtils;

    public TestAuthRunner(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public void run(String... args) {

        // 🔥 USER NORMAL
        String userUsername = "test123";   // adapte selon ta DB
        String userRole = "USER";

        String userToken = jwtUtils.generateToken(userUsername, userRole);

        // 🔥 ADMIN
        String adminUsername = "amal_nekh";  // adapte selon ta DB
        String adminRole = "ADMIN";

        String adminToken = jwtUtils.generateToken(adminUsername, adminRole);

        System.out.println("===================================");

        System.out.println("🔵 USER TOKEN:");
        System.out.println(userToken);

        System.out.println("-----------------------------------");

        System.out.println("🔴 ADMIN TOKEN:");
        System.out.println(adminToken);

        System.out.println("===================================");
    }
}