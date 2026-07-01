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
import com.study.study_platform.dto.ForgotPasswordRequest;
import com.study.study_platform.dto.VerifyCodeRequest;
import com.study.study_platform.dto.ResetPasswordRequest;
import com.study.study_platform.service.EmailService;


import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

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
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail())
                .orElse(null);

        // Toujours répondre OK même si l'email n'existe pas (évite l'énumération d'emails)
        if (utilisateur == null) {
            return ResponseEntity.ok(Map.of("message", "Si cet email existe, un code a été envoyé"));
        }

        String code = String.valueOf((int) (Math.random() * 900000) + 100000); // 6 chiffres
        utilisateur.setResetCode(code);
        utilisateur.setResetCodeExpiration(System.currentTimeMillis() + 10 * 60 * 1000); // 10 min
        utilisateurRepository.save(utilisateur);

        emailService.sendResetCode(utilisateur.getEmail(), code);

        return ResponseEntity.ok(Map.of("message", "Si cet email existe, un code a été envoyé"));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody VerifyCodeRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (utilisateur.getResetCode() == null ||
                !utilisateur.getResetCode().equals(request.getCode())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Code invalide"));
        }

        if (utilisateur.getResetCodeExpiration() == null ||
                utilisateur.getResetCodeExpiration() < System.currentTimeMillis()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Code expiré"));
        }

        return ResponseEntity.ok(Map.of("message", "Code valide"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (utilisateur.getResetCode() == null ||
                !utilisateur.getResetCode().equals(request.getCode())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Code invalide"));
        }

        if (utilisateur.getResetCodeExpiration() == null ||
                utilisateur.getResetCodeExpiration() < System.currentTimeMillis()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Code expiré"));
        }

        utilisateur.setPassword(passwordEncoder.encode(request.getNewPassword()));
        utilisateur.setResetCode(null);
        utilisateur.setResetCodeExpiration(null);
        utilisateurRepository.save(utilisateur);

        return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès"));
    }


}
