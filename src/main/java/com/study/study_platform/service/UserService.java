package com.study.study_platform.service;

import com.study.study_platform.dto.UserRequest;
import com.study.study_platform.dto.UserResponse;
import com.study.study_platform.exception.UserAlreadyExistsException;
import com.study.study_platform.model.document.Role;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.repository.UserRepository;
import com.study.study_platform.mapper.UserMapper;
import com.study.study_platform.exception.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(UserRepository utilisateurRepository,
                       PasswordEncoder passwordEncoder,
                       UserMapper userMapper) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    public Utilisateur createUtilisateur(UserRequest request) {

        // check username
        if (utilisateurRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        // check email
        if (utilisateurRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        Utilisateur user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPasswordMustChange(true);

        return utilisateurRepository.save(user);
    }

    public List<Utilisateur> getAllUtilisateurs() {
        return utilisateurRepository.findByRoleNot(Role.ADMIN);
    }

    public Utilisateur getUtilisateurById(String id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    public Utilisateur updateUtilisateur(String id, UserRequest request) {
        Utilisateur user = getUtilisateurById(id);

        // 🔹 check username unique (si changé)
        if (request.getUsername() != null &&
                !request.getUsername().equals(user.getUsername()) &&
                utilisateurRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        // 🔹 check email unique (si changé)
        if (request.getEmail() != null &&
                !request.getEmail().equals(user.getEmail()) &&
                utilisateurRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        // 🔹 update fields safely
        userMapper.updateEntity(user, request);

        // 🔹 password update
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return utilisateurRepository.save(user);
    }

    public void deleteUtilisateur(String id) {
        Utilisateur user = getUtilisateurById(id);
        utilisateurRepository.delete(user);
    }

    public void changePassword(String username, String oldPassword, String newPassword) {
        Utilisateur user = utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Ancien mot de passe incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordMustChange(false);

        utilisateurRepository.save(user);
    }

    public UserResponse getCurrentUser(String username) {
        Utilisateur user = utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        return userMapper.toResponse(user);
    }

    public Utilisateur updateByUsername(String username, UserRequest request) {
        Utilisateur user = utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        userMapper.updateEntity(user, request);

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return utilisateurRepository.save(user);
    }

    public void deleteByUsername(String username) {
        Utilisateur user = utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        utilisateurRepository.delete(user);
    }
}