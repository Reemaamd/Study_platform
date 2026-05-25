package com.study.study_platform.controller;

import com.study.study_platform.dto.OnboardingRequest;
import com.study.study_platform.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/onboarding")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class OnboardingController {

    private final OnboardingService onboardingService;

    @PostMapping
    public ResponseEntity<?> complete(
            @RequestBody OnboardingRequest request,
            Authentication authentication
    ) {

        onboardingService.process(
                authentication.getName(),
                request
        );

        return ResponseEntity.ok().build();
    }
}
