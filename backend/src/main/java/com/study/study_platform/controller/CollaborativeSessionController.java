package com.study.study_platform.controller;

import com.study.study_platform.dto.CommonAvailabilityDTO;
import com.study.study_platform.service.CollaborativeSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.List;
import com.study.study_platform.dto.CollaborativeSessionDTO;
import com.study.study_platform.model.document.StudySession;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/collaborative-sessions")
@RequiredArgsConstructor
public class CollaborativeSessionController {

    private final CollaborativeSessionService service;

    @GetMapping("/{groupId}/common-availabilities")
    @PreAuthorize("hasRole('USER')")
    public List<CommonAvailabilityDTO> getCommonAvailabilities(
            @PathVariable String groupId,
            Authentication authentication
    ) {

        return service.findCommonAvailabilities(
                groupId,
                authentication.getName()
        );
    }
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public StudySession createCollaborativeSession(
            @RequestBody CollaborativeSessionDTO dto,
            Authentication authentication
    ) {

        String username = authentication.getName();

        return service.createCollaborativeSession(
                dto,
                username
        );
    }
    @PatchMapping("/{id}/start")
    public ResponseEntity<?> startSession(
            @PathVariable String id,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                service.startSession(
                        id,
                        authentication.getName()
                )
        );
    }
    @PatchMapping("/{id}/complete")
    public ResponseEntity<?> completeSession(
            @PathVariable String id,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                service.completeSession(
                        id,
                        authentication.getName()
                )
        );
    }
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<?> cancelSession(
            @PathVariable String id,
            Authentication authentication
    ) {

        return ResponseEntity.ok(
                service.cancelSession(
                        id,
                        authentication.getName()
                )
        );
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSession(
            @PathVariable String id,
            Authentication authentication
    ) {

        service.deleteSession(
                id,
                authentication.getName()
        );

        return ResponseEntity.ok(
                "Session deleted successfully"
        );
    }

}