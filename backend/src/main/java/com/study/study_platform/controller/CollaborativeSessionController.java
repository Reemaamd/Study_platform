package com.study.study_platform.controller;

import com.study.study_platform.dto.CommonAvailabilityDTO;
import com.study.study_platform.service.CollaborativeSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/collaborative-sessions")
@RequiredArgsConstructor
public class CollaborativeSessionController {

    private final CollaborativeSessionService service;

    @GetMapping("/{groupId}/common-availabilities")
    @PreAuthorize("hasRole('USER')")
    public List<CommonAvailabilityDTO> getCommonAvailabilities(
            @PathVariable String groupId
    ) {

        return service.findCommonAvailabilities(groupId);
    }
}