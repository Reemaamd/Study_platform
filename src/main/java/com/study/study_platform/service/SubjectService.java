package com.study.study_platform.service;

import com.study.study_platform.dto.SubjectDTO;
import com.study.study_platform.dto.SubjectResponseDTO;
import com.study.study_platform.model.document.Subject;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.repository.SubjectRepository;
import com.study.study_platform.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@Service
public class SubjectService {

    private final SubjectRepository repository;
    private final UserRepository userRepository;

    public SubjectService(SubjectRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    // 🔥 Méthode utilitaire pour récupérer user connecté
    private Utilisateur getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // 🔥 Vérifier si ADMIN (pas autorisé ici)
    private void checkNotAdmin() {
        String role = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        if (role.equals("ROLE_ADMIN")) {
            throw new RuntimeException("Admin cannot access subjects");
        }
    }

    // ✅ CREATE sécurisé + anti-doublon
    public Subject createSubject(SubjectDTO dto) {

        checkNotAdmin(); // 🔒 bloquer admin

        Utilisateur user = getCurrentUser();

        if (repository.existsByNameAndUserId(dto.getName(), user.getId())) {
            throw new RuntimeException("Subject already exists for this user");
        }

        Subject subject = new Subject();
        subject.setName(dto.getName());
        subject.setUserId(user.getId());

        return repository.save(subject);
    }

    // ✅ GET sécurisé
    public List<SubjectResponseDTO> getSubjectsForCurrentUser() {

        checkNotAdmin(); // 🔒 bloquer admin

        Utilisateur user = getCurrentUser();

        return repository.findByUserId(user.getId())
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    // ✅ GET by name sécurisé
    public List<SubjectResponseDTO> getByName(String name) {

        checkNotAdmin(); // 🔒 bloquer admin

        Utilisateur user = getCurrentUser();

        return repository.findByNameAndUserId(name, user.getId())
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    // ✅ DELETE sécurisé
    public void deleteSubject(String id) {

        checkNotAdmin(); // 🔒 bloquer admin

        Utilisateur user = getCurrentUser();

        Subject subject = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        if (!subject.getUserId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access");
        }

        repository.deleteById(id);
    }

    // 🔥 Mapping DTO
    private SubjectResponseDTO mapToDTO(Subject subject) {

        Utilisateur user = userRepository.findById(subject.getUserId()).orElse(null);

        return new SubjectResponseDTO(
                subject.getId(),
                subject.getName(),
                user != null ? user.getUsername() : "unknown",
                user != null ? user.getName() : "unknown",
                user != null ? user.getEmail() : "unknown"
        );
    }
}