package com.study.study_platform.controller;


import com.study.study_platform.dto.UserRequest;
import com.study.study_platform.dto.UserResponse;
import com.study.study_platform.mapper.UserMapper;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // Seul l'admin peut accéder
public class AdminController {

    private final UserService utilisateurService;
    private final UserMapper userMapper;


    // Créer médecin / secrétaire
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest request) {
        Utilisateur user = utilisateurService.createUtilisateur(request);
        return ResponseEntity.ok(userMapper.toResponse(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String id) {
        Utilisateur user = utilisateurService.getUtilisateurById(id);
        return ResponseEntity.ok(userMapper.toResponse(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable String id,
            @RequestBody UserRequest request) {

        Utilisateur user = utilisateurService.updateUtilisateur(id, request);
        return ResponseEntity.ok(userMapper.toResponse(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable String id) {
        utilisateurService.deleteUtilisateur(id);
        return ResponseEntity.ok("Utilisateur supprimé avec succès");
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = utilisateurService.getAllUtilisateurs()
                .stream()
                .map(userMapper::toResponse)
                .toList();

        return ResponseEntity.ok(users);
    }
}

