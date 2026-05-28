package com.study.study_platform.service;


import com.study.study_platform.dto.*;
import com.study.study_platform.model.document.*;
import com.study.study_platform.model.enums.SessionStatus;
import com.study.study_platform.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.withinPercentage;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock private StudySessionRepository sessionRepository;
    @Mock private ObjectiveRepository    objectiveRepository;
    @Mock private SubjectRepository      subjectRepository;
    @Mock private UserRepository         userRepository;
    @Mock private GroupRepository        groupRepository;

    @InjectMocks
    private StatisticsService statisticsService;

    // ── fixtures ─────────────────────────────────────────────────────────────

    private static final String USERNAME = "alice";
    private static final String USER_ID  = "user-001";

    /** Returns a minimal Utilisateur stub. */
    private Utilisateur mockUser() {
        Utilisateur u = new Utilisateur();
        u.setId(USER_ID);
        u.setUsername(USERNAME);
        return u;
    }

    /**
     * Builds a StudySession whose start/end fall on the current Monday
     * so it is always inside the current ISO week.
     */
    private StudySession sessionThisWeek(SessionStatus status, long durationHours) {
        LocalDate monday = LocalDate.now().with(
                java.time.temporal.WeekFields.ISO.dayOfWeek(), 1);
        LocalDateTime start = monday.atTime(9, 0);
        LocalDateTime end   = start.plusHours(durationHours);

        StudySession s = new StudySession();
        s.setId(UUID.randomUUID().toString());
        s.setUserId(USER_ID);
        s.setStatus(status);
        s.setStartTime(start);
        s.setEndTime(end);
        return s;
    }

    /** Builds an Objective that belongs to the current ISO week. */
    private Objective objectiveThisWeek(int progress, int goal) {
        LocalDate monday = LocalDate.now().with(
                java.time.temporal.WeekFields.ISO.dayOfWeek(), 1);
        Objective o = new Objective();
        o.setUserId(USER_ID);
        o.setWeekStartDate(monday);
        o.setProgress(progress);
        o.setWeeklyGoal(goal);
        return o;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. getDashboard
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getDashboard – happy path returns correct aggregates")
    void getDashboard_happyPath() {
        // 3 sessions: 2 DONE, 1 PLANNED
        StudySession done1    = sessionThisWeek(SessionStatus.DONE, 2);
        StudySession done2    = sessionThisWeek(SessionStatus.DONE, 3);
        StudySession planned  = sessionThisWeek(SessionStatus.PLANNED, 1);

        // 3 objectives: 2 achieved, 1 not
        Objective obj1 = objectiveThisWeek(10, 5);  // achieved
        Objective obj2 = objectiveThisWeek(5,  5);  // achieved (== goal)
        Objective obj3 = objectiveThisWeek(2,  5);  // not achieved

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(mockUser()));
        when(sessionRepository.findByUserId(USER_ID))
                .thenReturn(List.of(done1, done2, planned));
        when(objectiveRepository.findByUserId(USER_ID))
                .thenReturn(List.of(obj1, obj2, obj3));

        DashboardStatsDTO result = statisticsService.getDashboard(USERNAME);

        assertThat(result.getTotalObjectives()).isEqualTo(3);
        assertThat(result.getAchievedObjectives()).isEqualTo(2);
        assertThat(result.getTotalSessions()).isEqualTo(3);
        assertThat(result.getCompletedSessions()).isEqualTo(2);
        assertThat(result.getTotalStudyHours()).isEqualTo(5L);          // 2+3
        assertThat(result.getCompletionRate()).isCloseTo(66.67, withinPercentage(1));
    }

    @Test
    @DisplayName("getDashboard – no sessions → rate is 0")
    void getDashboard_noSessions() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(mockUser()));
        when(sessionRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
        when(objectiveRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

        DashboardStatsDTO result = statisticsService.getDashboard(USERNAME);

        assertThat(result.getCompletionRate()).isEqualTo(0);
        assertThat(result.getTotalStudyHours()).isEqualTo(0);
    }

    @Test
    @DisplayName("getDashboard – user not found throws RuntimeException")
    void getDashboard_userNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> statisticsService.getDashboard("ghost"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. getStudyTime
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getStudyTime – sums planned/completed hours correctly")
    void getStudyTime_happyPath() {
        StudySession done      = sessionThisWeek(SessionStatus.DONE,      3);
        StudySession planned   = sessionThisWeek(SessionStatus.PLANNED,   2);
        StudySession cancelled = sessionThisWeek(SessionStatus.CANCELLED, 1);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(mockUser()));
        when(sessionRepository.findByUserId(USER_ID))
                .thenReturn(List.of(done, planned, cancelled));

        StudyTimeStatsDTO result = statisticsService.getStudyTime(USERNAME);

        // planned hours = DONE(3) + PLANNED(2) = 5  (CANCELLED excluded)
        assertThat(result.getPlannedHours()).isEqualTo(5L);
        assertThat(result.getCompletedHours()).isEqualTo(3L);
        assertThat(result.getRemainingHours()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getStudyTime – empty list returns zeros")
    void getStudyTime_empty() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(mockUser()));
        when(sessionRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

        StudyTimeStatsDTO result = statisticsService.getStudyTime(USERNAME);

        assertThat(result.getPlannedHours()).isZero();
        assertThat(result.getCompletedHours()).isZero();
        assertThat(result.getRemainingHours()).isZero();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. getSessionProgress
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getSessionProgress – computes done rate correctly")
    void getSessionProgress_happyPath() {
        StudySession done1   = sessionThisWeek(SessionStatus.DONE,      1);
        StudySession done2   = sessionThisWeek(SessionStatus.DONE,      1);
        StudySession planned = sessionThisWeek(SessionStatus.PLANNED,   1);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(mockUser()));
        when(sessionRepository.findByUserId(USER_ID))
                .thenReturn(List.of(done1, done2, planned));

        SessionProgressDTO result = statisticsService.getSessionProgress(USERNAME);

        assertThat(result.getPlannedSessions()).isEqualTo(1L);
        assertThat(result.getCompletedSessions()).isEqualTo(2L);
        // finished = done + cancelled etc. = 2; rate = 2/2 * 100
        assertThat(result.getCompletionRate()).isCloseTo(100.0, withinPercentage(1));
    }

    @Test
    @DisplayName("getSessionProgress – no finished sessions → rate is 0")
    void getSessionProgress_allPlanned() {
        StudySession planned = sessionThisWeek(SessionStatus.PLANNED, 1);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(mockUser()));
        when(sessionRepository.findByUserId(USER_ID)).thenReturn(List.of(planned));

        SessionProgressDTO result = statisticsService.getSessionProgress(USERNAME);

        assertThat(result.getCompletionRate()).isEqualTo(0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. getStudyTime – sessions with null times count as 0 hours
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getStudyTime – session with null startTime/endTime counts as 0h")
    void getStudyTime_nullTimes() {
        StudySession nullTime = new StudySession();
        nullTime.setUserId(USER_ID);
        nullTime.setStatus(SessionStatus.DONE);
        nullTime.setStartTime(LocalDate.now()
                .with(java.time.temporal.WeekFields.ISO.dayOfWeek(), 1).atTime(9, 0));
        nullTime.setEndTime(null);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(mockUser()));
        when(sessionRepository.findByUserId(USER_ID)).thenReturn(List.of(nullTime));

        StudyTimeStatsDTO result = statisticsService.getStudyTime(USERNAME);

        assertThat(result.getCompletedHours()).isEqualTo(0L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. getSubjectStats
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getSubjectStats – calculates done hours and percentage per subject")
    void getSubjectStats_happyPath() {
        Subject math = new Subject();
        math.setId("sub-math");
        math.setName("Mathematics");
        math.setUserId(USER_ID);

        StudySession done1 = sessionThisWeek(SessionStatus.DONE, 4);
        done1.setSubjectId("sub-math");

        StudySession planned = sessionThisWeek(SessionStatus.PLANNED, 4);
        planned.setSubjectId("sub-math");

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(mockUser()));
        when(subjectRepository.findByUserId(USER_ID)).thenReturn(List.of(math));
        when(sessionRepository.findByUserId(USER_ID)).thenReturn(List.of(done1, planned));

        List<SubjectStatsDTO> result = statisticsService.getSubjectStats(USERNAME);

        assertThat(result).hasSize(1);
        SubjectStatsDTO stats = result.get(0);
        assertThat(stats.getSubjectName()).isEqualTo("Mathematics");
        assertThat(stats.getTotalHours()).isEqualTo(4L);
        assertThat(stats.getPercentage()).isEqualTo(50.0); // 4 done / (4+4) total
    }

    @Test
    @DisplayName("getSubjectStats – subject with no sessions has 0% percentage")
    void getSubjectStats_noSessions() {
        Subject physics = new Subject();
        physics.setId("sub-phy");
        physics.setName("Physics");
        physics.setUserId(USER_ID);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(mockUser()));
        when(subjectRepository.findByUserId(USER_ID)).thenReturn(List.of(physics));
        when(sessionRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

        List<SubjectStatsDTO> result = statisticsService.getSubjectStats(USERNAME);

        assertThat(result.get(0).getPercentage()).isEqualTo(0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. getDailyHours
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getDailyHours – returns 7 entries, one per day of week")
    void getDailyHours_sevenEntries() {
        StudySession done = sessionThisWeek(SessionStatus.DONE, 2);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(mockUser()));
        when(sessionRepository.findByUserId(USER_ID)).thenReturn(List.of(done));

        List<DailyStudyHoursDTO> result = statisticsService.getDailyHours(USERNAME);

        assertThat(result).hasSize(7);
        List<String> days = result.stream().map(DailyStudyHoursDTO::getDay).toList();
        assertThat(days).containsExactly(
                "MONDAY","TUESDAY","WEDNESDAY",
                "THURSDAY","FRIDAY","SATURDAY","SUNDAY");
    }

    @Test
    @DisplayName("getDailyHours – correct hours on Monday")
    void getDailyHours_mondayHours() {
        // sessionThisWeek always places session on Monday
        StudySession done = sessionThisWeek(SessionStatus.DONE, 3);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(mockUser()));
        when(sessionRepository.findByUserId(USER_ID)).thenReturn(List.of(done));

        List<DailyStudyHoursDTO> result = statisticsService.getDailyHours(USERNAME);

        DailyStudyHoursDTO monday = result.stream()
                .filter(d -> d.getDay().equals("MONDAY"))
                .findFirst().orElseThrow();
        assertThat(monday.getHours()).isEqualTo(3L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. getObjectiveCompletion
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getObjectiveCompletion – rate = 50% when half achieved")
    void getObjectiveCompletion_halfAchieved() {
        Objective achieved = objectiveThisWeek(10, 5);
        Objective notYet   = objectiveThisWeek(2,  5);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(mockUser()));
        when(objectiveRepository.findByUserId(USER_ID))
                .thenReturn(List.of(achieved, notYet));

        ObjectiveCompletionDTO result = statisticsService.getObjectiveCompletion(USERNAME);

        assertThat(result.getTotalObjectives()).isEqualTo(2);
        assertThat(result.getAchievedObjectives()).isEqualTo(1);
        assertThat(result.getCompletionRate()).isCloseTo(50.0, withinPercentage(1));
    }

    @Test
    @DisplayName("getObjectiveCompletion – no objectives → rate is 0")
    void getObjectiveCompletion_empty() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(mockUser()));
        when(objectiveRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

        ObjectiveCompletionDTO result = statisticsService.getObjectiveCompletion(USERNAME);

        assertThat(result.getCompletionRate()).isEqualTo(0);
        assertThat(result.getTotalObjectives()).isEqualTo(0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. getCurrentWeek
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getCurrentWeek – aggregates hours, sessions, objectives correctly")
    void getCurrentWeek_happyPath() {
        StudySession done = sessionThisWeek(SessionStatus.DONE, 5);
        Objective achieved = objectiveThisWeek(6, 5);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(mockUser()));
        when(sessionRepository.findByUserId(USER_ID)).thenReturn(List.of(done));
        when(objectiveRepository.findByUserId(USER_ID)).thenReturn(List.of(achieved));

        CurrentWeekStatsDTO result = statisticsService.getCurrentWeek(USERNAME);

        assertThat(result.getStudyHours()).isEqualTo(5L);
        assertThat(result.getCompletedSessions()).isEqualTo(1L);
        assertThat(result.getAchievedObjectives()).isEqualTo(1L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 9. getAdminDashboard
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAdminDashboard – sums all users and sessions")
    void getAdminDashboard_happyPath() {
        Utilisateur user1 = new Utilisateur();
        user1.setId("u1");
        user1.setRole(Role.USER);

        Utilisateur user2 = new Utilisateur();
        user2.setId("u2");
        user2.setRole(Role.USER);

        StudySession done = new StudySession();
        done.setUserId("u1");
        done.setStatus(SessionStatus.DONE);
        done.setStartTime(LocalDateTime.now().minusHours(2));
        done.setEndTime(LocalDateTime.now());

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));
        when(sessionRepository.findAll()).thenReturn(List.of(done));

        Map<String, Object> result = statisticsService.getAdminDashboard();

        assertThat(result.get("totalUsers")).isEqualTo(2L);
        assertThat(result.get("totalSessions")).isEqualTo(1L);
        assertThat(result.get("completedSessions")).isEqualTo(1L);
    }

    @Test
    @DisplayName("getAdminDashboard – ADMIN users are excluded from totalUsers")
    void getAdminDashboard_adminExcluded() {
        Utilisateur admin = new Utilisateur();
        admin.setId("admin1");
        admin.setRole(Role.ADMIN);

        when(userRepository.findAll()).thenReturn(List.of(admin));
        when(sessionRepository.findAll()).thenReturn(Collections.emptyList());

        Map<String, Object> result = statisticsService.getAdminDashboard();

        assertThat(result.get("totalUsers")).isEqualTo(0L);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 10. getWeeklyTrend
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getWeeklyTrend – groups sessions by ISO week label")
    void getWeeklyTrend_groupsByWeek() {
        StudySession s = new StudySession();
        s.setUserId(USER_ID);
        s.setStatus(SessionStatus.DONE);
        s.setStartTime(LocalDateTime.of(2025, 1, 6, 10, 0));  // Monday W02
        s.setEndTime(LocalDateTime.of(2025, 1, 6, 12, 0));

        when(sessionRepository.findAll()).thenReturn(List.of(s));

        List<Map<String, Object>> result = statisticsService.getWeeklyTrend();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("week")).isEqualTo("2025-W02");
        assertThat(result.get(0).get("sessions")).isEqualTo(1L);
        assertThat(result.get(0).get("hours")).isEqualTo(2L);
    }

    @Test
    @DisplayName("getWeeklyTrend – sessions with null startTime are skipped")
    void getWeeklyTrend_nullStartTimeSkipped() {
        StudySession nullStart = new StudySession();
        nullStart.setStartTime(null);

        when(sessionRepository.findAll()).thenReturn(List.of(nullStart));

        List<Map<String, Object>> result = statisticsService.getWeeklyTrend();

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 11. getAdminSubjectsStats
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAdminSubjectsStats – filters out subjects with 0 hours")
    void getAdminSubjectsStats_filtersZeroHours() {
        Subject subA = new Subject(); subA.setId("sA"); subA.setName("SubjectA");
        Subject subB = new Subject(); subB.setId("sB"); subB.setName("SubjectB");

        StudySession done = new StudySession();
        done.setSubjectId("sA");
        done.setStatus(SessionStatus.DONE);
        done.setStartTime(LocalDateTime.now().minusHours(3));
        done.setEndTime(LocalDateTime.now());

        when(subjectRepository.findAll()).thenReturn(List.of(subA, subB));
        when(sessionRepository.findAll()).thenReturn(List.of(done));

        List<Map<String, Object>> result = statisticsService.getAdminSubjectsStats();

        // subB has 0 hours → must be filtered out
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("subject")).isEqualTo("SubjectA");
    }

    @Test
    @DisplayName("getAdminSubjectsStats – sorted by totalHours descending")
    void getAdminSubjectsStats_sortedByHours() {
        Subject subA = new Subject(); subA.setId("sA"); subA.setName("A");
        Subject subB = new Subject(); subB.setId("sB"); subB.setName("B");

        StudySession s1 = new StudySession();
        s1.setSubjectId("sA"); s1.setStatus(SessionStatus.DONE);
        s1.setStartTime(LocalDateTime.now().minusHours(2));
        s1.setEndTime(LocalDateTime.now());

        StudySession s2 = new StudySession();
        s2.setSubjectId("sB"); s2.setStatus(SessionStatus.DONE);
        s2.setStartTime(LocalDateTime.now().minusHours(5));
        s2.setEndTime(LocalDateTime.now());

        when(subjectRepository.findAll()).thenReturn(List.of(subA, subB));
        when(sessionRepository.findAll()).thenReturn(List.of(s1, s2));

        List<Map<String, Object>> result = statisticsService.getAdminSubjectsStats();

        assertThat(result.get(0).get("subject")).isEqualTo("B");
        assertThat(result.get(1).get("subject")).isEqualTo("A");
    }
}
