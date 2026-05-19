package com.study.study_platform.controller;

import com.study.study_platform.dto.InvitationDTO;
import com.study.study_platform.dto.InvitationResponseDTO;
import com.study.study_platform.model.document.Invitation;
import com.study.study_platform.service.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public InvitationResponseDTO sendInvitation(
            @RequestBody InvitationDTO dto,
            Authentication authentication
    ) {

        String username = authentication.getName();

        return invitationService.sendInvitation(dto, username);
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public List<Invitation> getMyInvitations(
            Authentication authentication
    ) {

        String username = authentication.getName();

        return invitationService.getMyInvitations(username);
    }

    @PostMapping("/{invitationId}/accept")
    @PreAuthorize("hasRole('USER')")
    public String acceptInvitation(
            @PathVariable String invitationId,
            Authentication authentication
    ) {

        String username = authentication.getName();

        return invitationService.acceptInvitation(invitationId, username);
    }

    @PostMapping("/{invitationId}/reject")
    @PreAuthorize("hasRole('USER')")
    public String rejectInvitation(
            @PathVariable String invitationId,
            Authentication authentication
    ) {

        String username = authentication.getName();

        return invitationService.rejectInvitation(invitationId, username);
    }
}