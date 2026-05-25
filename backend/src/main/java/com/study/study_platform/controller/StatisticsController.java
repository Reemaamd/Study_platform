package com.study.study_platform.controller;

import com.study.study_platform.dto.*;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.repository.UserRepository;
import com.study.study_platform.service.StatisticsService;
import com.study.study_platform.service.StudySessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class StatisticsController {

    private final StatisticsService service;
    private final StudySessionService sessionService;
    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    public DashboardStatsDTO dashboard(
            @AuthenticationPrincipal UserDetails u) {
        return service.getDashboard(u.getUsername());
    }

    @GetMapping("/study-time")
    public StudyTimeStatsDTO studyTime(
            @AuthenticationPrincipal UserDetails u) {
        return service.getStudyTime(u.getUsername());
    }

    @GetMapping("/progress")
    public SessionProgressDTO progress(
            @AuthenticationPrincipal UserDetails u) {
        return service.getSessionProgress(u.getUsername());
    }

    @GetMapping("/weekly-productivity")
    public List<WeeklyProductivityDTO> weeklyProductivity(
            @AuthenticationPrincipal UserDetails u) {
        return service.getWeeklyProductivity(u.getUsername());
    }

    // ✅ Cette méthode doit appeler getSubjectStats(username), PAS getSubjectsStats()
    @GetMapping("/subjects-stats")
    public List<SubjectStatsDTO> subjectsStats(
            @AuthenticationPrincipal UserDetails u) {
        return service.getSubjectStats(u.getUsername());  // ← avec "username" en param
    }
   //Est-ce que l’utilisateur travaille régulièrement ?
    //“habitude globale sur toutes les données”
    @GetMapping("/daily-hours")
    public List<DailyStudyHoursDTO> dailyHours(
            @AuthenticationPrincipal UserDetails u) {
        return service.getDailyHours(u.getUsername());
    }

    @GetMapping("/objective-completion")
    public ObjectiveCompletionDTO objectiveCompletion(
            @AuthenticationPrincipal UserDetails u) {
        return service.getObjectiveCompletion(u.getUsername());
    }

    @GetMapping("/current-week")
    public CurrentWeekStatsDTO currentWeek(
            @AuthenticationPrincipal UserDetails u) {
        return service.getCurrentWeek(u.getUsername());
    }
    //admin statistic
    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> adminDashboard() {
        return service.getAdminDashboard();
    }
    @GetMapping("/admin/users-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> usersStats() {
        return service.getUsersStats();
    }
    /*@GetMapping("/admin/subjects-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> subjectsStats() {
        return service.getSubjectsStats();
    }*/
    @GetMapping("/admin/weekly-trend")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> weeklyTrend() {
        return service.getWeeklyTrend();
    }

    @GetMapping("/today-sessions")
    public List<TodaySessionDTO> todaySessions(
            @AuthenticationPrincipal UserDetails u) {

        return service.getTodaySessions(u.getUsername());
    }

    @GetMapping("/streak")
    public StreakResponse getStreak(@AuthenticationPrincipal UserDetails u) {

        String username = u.getUsername();


        Utilisateur user = userRepository.findByUsername(username)
                .orElseThrow();

        int streak = sessionService.calculateFocusStreak(user.getId());

        return new StreakResponse(streak);
    // 1) Tous les utilisateurs (USER + ADMIN) — pour la table users
    @GetMapping("/admin/all-users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> getAllUsers() {
        return service.getAllUsers();
    }
    // 2) Tous les groupes — pour la table groups (admin view)
    @GetMapping("/admin/groups")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> getAllGroups() {
        return service.getAllGroupsForAdmin();
    }
}