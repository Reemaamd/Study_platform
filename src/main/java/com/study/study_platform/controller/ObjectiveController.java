package com.study.study_platform.controller;

import com.study.study_platform.dto.ObjectiveDTO;
import com.study.study_platform.dto.ObjectiveRequest;
import com.study.study_platform.service.ObjectiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.DayOfWeek;
import java.util.List;
import java.time.LocalDate;
import java.util.Map;


@RestController
@RequestMapping("/objectives")
@RequiredArgsConstructor
public class ObjectiveController {

    private final ObjectiveService objectiveService;

    // CREATE
    @PostMapping
    public ObjectiveDTO create(@RequestBody ObjectiveRequest request) {
        return objectiveService.create(request);
    }

    // GET ALL
    @GetMapping
    public List<ObjectiveDTO> getAll() {
        return objectiveService.getAll();
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ObjectiveDTO getById(@PathVariable String id) {
        return objectiveService.getById(id);
    }

    // UPDATE PROGRESS
    @PatchMapping("/{id}/progress")
    public ObjectiveDTO updateProgress(
            @PathVariable String id,
            @RequestParam int progress
    ) {
        return objectiveService.updateProgress(id, progress);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        objectiveService.delete(id);
    }
    // UPDATE
    @PutMapping("/{id}")
    public ObjectiveDTO update(@PathVariable String id, @RequestBody ObjectiveRequest request) {
        return objectiveService.update(id, request);
    }

    // GET BY PRIORITY
    @GetMapping("/priority")
    public List<ObjectiveDTO> getByPriority() {
        return objectiveService.getByPriority();
    }

    // GET ACHIEVED
    @GetMapping("/achieved")
    public List<ObjectiveDTO> getAchieved() {
        return objectiveService.getAchieved();
    }

    @GetMapping("/week")
    public List<ObjectiveDTO> getByWeek(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        LocalDate start = date.with(DayOfWeek.MONDAY);
        LocalDate end = date.with(DayOfWeek.SUNDAY);

        return objectiveService.getByWeek(start, end);
    }
    @GetMapping("/stats")
    public Map<String, Long> getStats() {
        return Map.of(
                "total", objectiveService.getTotalObjectives(),
                "achieved", objectiveService.getAchievedCount(),
                "pending", objectiveService.getPendingCount()
        );
    }
    @GetMapping("/weekly-overview")
    public List<ObjectiveDTO> getWeeklyOverview(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        return objectiveService.getWeeklyOverview(date);
    }
    // ADMIN ENDPOINTS
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ObjectiveDTO> getAllForAdmin() {
        return objectiveService.getAllForAdmin();
    }

    @GetMapping("/admin/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ObjectiveDTO> getByUserForAdmin(@PathVariable String userId) {
        return objectiveService.getByUserIdForAdmin(userId);
    }

    @GetMapping("/admin/subject/{subjectId}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ObjectiveDTO> getBySubjectForAdmin(@PathVariable String subjectId) {
        return objectiveService.getBySubjectForAdmin(subjectId);
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getGlobalStats() {
        return objectiveService.getGlobalStats();
    }

    // USER - historique
    @GetMapping("/history")
    public List<ObjectiveDTO> getHistory() {
        return objectiveService.getHistory();
    }
}