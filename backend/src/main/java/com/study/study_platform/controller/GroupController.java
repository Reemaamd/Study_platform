package com.study.study_platform.controller;
import jakarta.validation.Valid;
import com.study.study_platform.dto.GroupDTO;
import com.study.study_platform.dto.GroupResponseDTO;
import com.study.study_platform.model.document.Group;
import com.study.study_platform.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public GroupResponseDTO createGroup(
            @Valid @RequestBody GroupDTO dto,
            Authentication authentication
    ) {

        String username = authentication.getName();

        return groupService.createGroup(dto, username);
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public List<Group> getMyGroups(Authentication authentication) {

        String username = authentication.getName();

        return groupService.getMyGroups(username);
    }

    @GetMapping("/{groupId}")
    @PreAuthorize("hasRole('USER')")
    public Group getGroupDetails(@PathVariable String groupId) {

        return groupService.getGroupDetails(groupId);
    }

    @PostMapping("/{groupId}/leave")
    @PreAuthorize("hasRole('USER')")
    public String leaveGroup(
            @PathVariable String groupId,
            Authentication authentication
    ) {

        String username = authentication.getName();

        groupService.leaveGroup(groupId, username);

        return "You left the group";
    }

    @DeleteMapping("/{groupId}")
    @PreAuthorize("hasRole('USER')")
    public String deleteGroup(
            @PathVariable String groupId,
            Authentication authentication
    ) {

        String username = authentication.getName();

        groupService.deleteGroup(groupId, username);

        return "Group deleted";
    }
    @PutMapping("/{groupId}")
    @PreAuthorize("hasRole('USER')")
    public GroupResponseDTO updateGroup(
            @PathVariable String groupId,
            @RequestBody GroupDTO dto,
            Authentication authentication
    ) {

        String username = authentication.getName();

        return groupService.updateGroup(groupId, dto, username);
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    @PreAuthorize("hasRole('USER')")
    public String removeMember(
            @PathVariable String groupId,
            @PathVariable String memberId,
            Authentication authentication
    ) {

        String username = authentication.getName();

        groupService.removeMember(groupId, memberId, username);

        return "Member removed successfully";
    }
}