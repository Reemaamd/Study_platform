package com.study.study_platform.controller;

import com.study.study_platform.dto.AvailabilityDTO;
import com.study.study_platform.model.embedded.Availability;
import com.study.study_platform.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users/availabilities")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class AvailabilityController {

    private final AvailabilityService service;

    @GetMapping
    public List<Availability> getUserAvailability(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return service.getUserAvailability(userDetails.getUsername());
    }

    @PostMapping
    public List<Availability> addAvailability(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody AvailabilityDTO dto
    ) {
        return service.addAvailability(userDetails.getUsername(), dto);
    }

    @PutMapping("/{index}")
    public List<Availability> updateAvailability(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable int index,
            @RequestBody AvailabilityDTO dto
    ) {
        return service.updateAvailability(userDetails.getUsername(), index, dto);
    }

    @DeleteMapping("/{index}")
    public void deleteAvailability(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable int index
    ) {
        service.deleteAvailability(userDetails.getUsername(), index);
    }
}