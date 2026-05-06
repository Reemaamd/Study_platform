package com.study.study_platform.controller;

import com.study.study_platform.model.document.StudySession;
import com.study.study_platform.service.StudySessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PostMapping("/generate")
    public List<StudySession> generate(
            @AuthenticationPrincipal UserDetails userDetails) {

        return service.generateWeeklyPlan(userDetails.getUsername());
    }
    @PutMapping("/{id}/complete")
    public StudySession completeSession(@PathVariable String id) {
        return service.completeSession(id);
    }
}