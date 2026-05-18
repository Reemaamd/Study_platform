package com.study.study_platform.controller;

import com.study.study_platform.dto.UserRequest;
import com.study.study_platform.dto.UserResponse;
import com.study.study_platform.mapper.UserMapper;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping("/me")
    public UserResponse getProfile(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return userService.getCurrentUser(userDetails.getUsername());
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserRequest request
    ) {
        Utilisateur user = userService.updateByUsername(userDetails.getUsername(), request);
        return ResponseEntity.ok(userMapper.toResponse(user));
    }

    @DeleteMapping("/me")
    public ResponseEntity<String> deleteCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        userService.deleteByUsername(userDetails.getUsername());
        return ResponseEntity.ok("Utilisateur supprimé avec succès");
    }

    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @RequestParam String username,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {

        userService.changePassword(username, oldPassword, newPassword);
        return ResponseEntity.ok("Mot de passe modifié avec succès");
    }
}