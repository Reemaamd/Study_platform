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
    public void run(String... args) throws Exception {

        // 🔥 simuler un login (comme si user est authentifié)
        String username = "amal_nekh";
        String role = "ADMIN";

        // 🔥 générer token directement
        String token = jwtUtils.generateToken(username, role);

        System.out.println("===================================");
        System.out.println("TOKEN JWT POUR TEST:");
        System.out.println(token);
        System.out.println("===================================");
    }
}