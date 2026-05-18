package com.study.study_platform.controller;

import com.study.study_platform.dto.JwtResponse;
import com.study.study_platform.dto.LoginRequest;
import com.study.study_platform.dto.RegisterRequest;
import com.study.study_platform.model.document.Role;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.repository.UserRepository;
import com.study.study_platform.security.JwtUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;



import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            UserDetails userDetails = (UserDetails) auth.getPrincipal();

            Utilisateur utilisateur = utilisateurRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

            String role = utilisateur.getRole().name();

            String token = jwtUtils.generateToken(userDetails.getUsername(), role);

            return ResponseEntity.ok(new JwtResponse(token, role, utilisateur.getUsername()));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login ou mot de passe incorrect"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        if (utilisateurRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        if (utilisateurRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }


        Utilisateur user = new Utilisateur();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        utilisateurRepository.save(user);

        return ResponseEntity.ok("User registered successfully");
    }


}
