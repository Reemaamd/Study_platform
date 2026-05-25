package com.study.study_platform.controller;

import com.study.study_platform.dto.AdminGroupDTO;
import com.study.study_platform.service.AdminGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/groups")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor

public class AdminGroupController {

    private final AdminGroupService adminGroupService;
    @GetMapping
    public List<AdminGroupDTO> getAllGroups() {
        return adminGroupService.getAllGroups();
    }
}
