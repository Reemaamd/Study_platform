package com.study.study_platform.controller;

import com.study.study_platform.dto.StudySessionDTO;
import com.study.study_platform.model.document.StudySession;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.repository.StudySessionRepository;
import com.study.study_platform.repository.UserRepository;
import com.study.study_platform.service.StudySessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/study-sessions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class StudySessionController {

    private final StudySessionService service;
    private final UserRepository userRepository;


    @PostMapping("/generate")
    public List<StudySession> generate(
            @AuthenticationPrincipal UserDetails userDetails) {

        return service.generateWeeklyPlan(userDetails.getUsername());
    }

    @PutMapping("/{id}/complete")
    public StudySession completeSession(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {

        // ✅ Récupérer l'ID réel depuis le username
        Utilisateur user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return service.completeSession(id, user.getId()); // ✅ passer l'ID, pas le username
    }

    @GetMapping
    public List<StudySessionDTO> getUserSessions(
            @AuthenticationPrincipal UserDetails u,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        return service.getUserSessions(u.getUsername(), startDate, endDate);
    }
}