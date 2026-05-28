package com.study.study_platform.service;

import com.study.study_platform.dto.*;
import com.study.study_platform.model.document.*;
import com.study.study_platform.model.enums.SessionStatus;
import com.study.study_platform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.IsoFields;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final StudySessionRepository sessionRepository;
    private final ObjectiveRepository objectiveRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    // ─────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────
    private Utilisateur getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private String getWeekLabel(LocalDate date) {
        int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int year = date.get(IsoFields.WEEK_BASED_YEAR);
        return year + "-W" + String.format("%02d", week);
    }

    private long hoursOf(StudySession s) {
        if (s.getStartTime() == null || s.getEndTime() == null) return 0;
        return Duration.between(s.getStartTime(), s.getEndTime()).toHours();
    }

    private LocalDate currentMonday() {
        return LocalDate.now().with(WeekFields.ISO.dayOfWeek(), 1);
    }

    private LocalDate currentSunday() {
        return LocalDate.now().with(WeekFields.ISO.dayOfWeek(), 7);
    }

    private List<StudySession> filterCurrentWeek(List<StudySession> sessions) {
        LocalDate start = currentMonday();
        LocalDate end   = currentSunday();
        return sessions.stream()
                .filter(s -> s.getStartTime() != null)
                .filter(s -> {
                    LocalDate d = s.getStartTime().toLocalDate();
                    return !d.isBefore(start) && !d.isAfter(end);
                })
                .toList();
    }

    private List<Objective> filterCurrentWeekObjectives(List<Objective> objectives) {
        LocalDate start = currentMonday();
        LocalDate end   = currentSunday();
        return objectives.stream()
                .filter(o -> o.getWeekStartDate() != null)
                .filter(o -> !o.getWeekStartDate().isBefore(start)
                        && !o.getWeekStartDate().isAfter(end))
                .toList();
    }

    // ─────────────────────────────────────────
    // 1. DASHBOARD GLOBAL — semaine courante
    // ─────────────────────────────────────────
    public DashboardStatsDTO getDashboard(String username) {

        String userId = getUser(username).getId();

        List<StudySession> sessions = filterCurrentWeek(
                sessionRepository.findByUserId(userId)
        );

        List<Objective> objectives = filterCurrentWeekObjectives(
                objectiveRepository.findByUserId(userId)
        );

        long totalSessions = sessions.size();

        long completedSessions = sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.DONE)
                .count();

        long totalHours = sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.DONE)
                .mapToLong(this::hoursOf)
                .sum();

        long totalObjectives = objectives.size();

        long achieved = objectives.stream()
                .filter(o -> o.getProgress() >= o.getWeeklyGoal())
                .count();

        double rate = totalSessions == 0 ? 0 :
                (double) completedSessions / totalSessions * 100;

        return new DashboardStatsDTO(
                totalObjectives,
                achieved,
                totalSessions,
                completedSessions,
                totalHours,
                rate
        );
    }

    // ─────────────────────────────────────────
    // 2. STUDY TIME — semaine courante
    // ─────────────────────────────────────────
    public StudyTimeStatsDTO getStudyTime(String username) {

        String userId = getUser(username).getId();

        List<StudySession> sessions = filterCurrentWeek(
                sessionRepository.findByUserId(userId)
        );

        long planned = sessions.stream()
                .filter(s -> s.getStatus() != SessionStatus.CANCELLED)
                .mapToLong(this::hoursOf)
                .sum();

        long completed = sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.DONE)
                .mapToLong(this::hoursOf)
                .sum();

        return new StudyTimeStatsDTO(planned, completed, planned - completed);
    }

    // ─────────────────────────────────────────
    // 3. SESSION PROGRESS — semaine courante
    // ─────────────────────────────────────────
    public SessionProgressDTO getSessionProgress(String username) {

        String userId = getUser(username).getId();

        List<StudySession> sessions = filterCurrentWeek(
                sessionRepository.findByUserId(userId)
        );

        long planned = sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.PLANNED)
                .count();

        long done = sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.DONE)
                .count();

        long finished = sessions.stream()
                .filter(s -> s.getStatus() != SessionStatus.PLANNED)
                .count();

        double rate = finished == 0 ? 0 :
                (double) done / finished * 100;

        return new SessionProgressDTO(planned, done, rate);
    }

    // ─────────────────────────────────────────
    // 4. WEEKLY PRODUCTIVITY — semaine courante
    // ─────────────────────────────────────────
    public List<WeeklyProductivityDTO> getWeeklyProductivity(String username) {

        String userId = getUser(username).getId();

        List<StudySession> sessions = filterCurrentWeek(
                sessionRepository.findByUserId(userId)
        );
        List<Objective> objectives = filterCurrentWeekObjectives(
                objectiveRepository.findByUserId(userId)
        );

        Map<String, long[]> weekMap = new TreeMap<>();

        for (StudySession s : sessions) {
            if (s.getStatus() != SessionStatus.DONE) continue;
            String label = getWeekLabel(s.getStartTime().toLocalDate());
            weekMap.computeIfAbsent(label, k -> new long[]{0, 0});
            weekMap.get(label)[0] += hoursOf(s);
            weekMap.get(label)[1]++;
        }

        return weekMap.entrySet().stream().map(e -> {
            String week = e.getKey();
            long achievedObj = objectives.stream()
                    .filter(o -> getWeekLabel(o.getWeekStartDate()).equals(week)
                            && o.getProgress() >= o.getWeeklyGoal())
                    .count();
            return new WeeklyProductivityDTO(week, e.getValue()[0], e.getValue()[1], achievedObj);
        }).toList();
    }

    // ─────────────────────────────────────────
    // 5. SUBJECT STATS — semaine courante (utilisateur connecté)
    // ─────────────────────────────────────────
    public List<SubjectStatsDTO> getSubjectStats(String username) {

        Utilisateur user = getUser(username);
        List<Subject> subjects = subjectRepository.findByUserId(user.getId());

        List<StudySession> sessions = filterCurrentWeek(
                sessionRepository.findByUserId(user.getId())
        );

        long totalDoneHours = sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.DONE)
                .mapToLong(this::hoursOf)
                .sum();

        return subjects.stream().map(subject -> {

            List<StudySession> subjectSessions = sessions.stream()
                    .filter(s -> subject.getId().equals(s.getSubjectId()))
                    .toList();

            long doneHours = subjectSessions.stream()
                    .filter(s -> s.getStatus() == SessionStatus.DONE)
                    .mapToLong(this::hoursOf)
                    .sum();

            long plannedHours = subjectSessions.stream()
                    .filter(s -> s.getStatus() != SessionStatus.DONE)
                    .mapToLong(this::hoursOf)
                    .sum();

            long totalSubjectHours = doneHours + plannedHours;
            double percentage = totalSubjectHours > 0
                    ? Math.round((doneHours * 100.0) / totalSubjectHours)
                    : 0;

            return new SubjectStatsDTO(subject.getName(), doneHours, percentage);

        }).toList();
    }

    // ─────────────────────────────────────────
    // 6. DAILY HOURS — semaine courante
    // ─────────────────────────────────────────
    public List<DailyStudyHoursDTO> getDailyHours(String username) {

        String userId = getUser(username).getId();

        Map<String, Long> map = filterCurrentWeek(
                sessionRepository.findByUserId(userId)
        ).stream()
                .filter(s -> s.getStatus() == SessionStatus.DONE)
                .collect(Collectors.groupingBy(
                        s -> s.getStartTime().getDayOfWeek().name(),
                        Collectors.summingLong(this::hoursOf)
                ));

        List<String> order = List.of(
                "MONDAY","TUESDAY","WEDNESDAY",
                "THURSDAY","FRIDAY","SATURDAY","SUNDAY"
        );

        return order.stream()
                .map(day -> new DailyStudyHoursDTO(day, map.getOrDefault(day, 0L)))
                .toList();
    }

    // ─────────────────────────────────────────
    // 7. OBJECTIVE COMPLETION — semaine courante
    // ─────────────────────────────────────────
    public ObjectiveCompletionDTO getObjectiveCompletion(String username) {

        String userId = getUser(username).getId();

        List<Objective> objectives = filterCurrentWeekObjectives(
                objectiveRepository.findByUserId(userId)
        );

        long achieved = objectives.stream()
                .filter(o -> o.getProgress() >= o.getWeeklyGoal())
                .count();

        double rate = objectives.isEmpty() ? 0 :
                (double) achieved / objectives.size() * 100;

        return new ObjectiveCompletionDTO(objectives.size(), achieved, rate);
    }

    // ─────────────────────────────────────────
    // 8. CURRENT WEEK
    // ─────────────────────────────────────────
    public CurrentWeekStatsDTO getCurrentWeek(String username) {

        String userId = getUser(username).getId();

        List<StudySession> sessions = filterCurrentWeek(
                sessionRepository.findByUserId(userId)
        );

        long hours = sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.DONE)
                .mapToLong(this::hoursOf)
                .sum();

        long completedSessions = sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.DONE)
                .count();

        long achievedObjectives = filterCurrentWeekObjectives(
                objectiveRepository.findByUserId(userId)
        ).stream()
                .filter(o -> o.getProgress() >= o.getWeeklyGoal())
                .count();

        return new CurrentWeekStatsDTO(hours, completedSessions, achievedObjectives);
    }

    // ─────────────────────────────────────────
    // TODAY SESSIONS
    // ─────────────────────────────────────────
    public List<TodaySessionDTO> getTodaySessions(String username) {

        Utilisateur user = getUser(username);
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        List<StudySession> sessions = sessionRepository
                .findByUserIdAndStartTimeBetween(user.getId(), startOfDay, endOfDay);

        LocalTime now = LocalTime.now();

        return sessions.stream().map(session -> {
            boolean active = now.isAfter(session.getStartTime().toLocalTime())
                    && now.isBefore(session.getEndTime().toLocalTime());

            Subject subject = subjectRepository.findById(session.getSubjectId())
                    .orElseThrow();

            return new TodaySessionDTO(
                    session.getId(),
                    subject.getName(),
                    session.getStartTime().toLocalTime().toString(),
                    session.getEndTime().toLocalTime().toString(),
                    active,
                    "green"
            );
        }).toList();
    }

    // ─────────────────────────────────────────
    // ADMIN
    // ─────────────────────────────────────────
    public Map<String, Object> getAdminDashboard() {
        List<Utilisateur> users = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.USER).toList();
        List<StudySession> sessions = sessionRepository.findAll();

        long totalUsers = users.size();
        long totalSessions = sessions.size();
        long completedSessions = sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.DONE).count();
        long totalStudyHours = sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.DONE)
                .mapToLong(this::hoursOf).sum();

        LocalDateTime limit = LocalDateTime.now().minusDays(7);
        long activeUsers = sessions.stream()
                .filter(s -> s.getStartTime() != null && s.getStartTime().isAfter(limit))
                .map(StudySession::getUserId).distinct().count();

        double avgStudyPerUser = totalUsers == 0 ? 0 : (double) totalStudyHours / totalUsers;

        Map<String, Object> res = new HashMap<>();
        res.put("totalUsers", totalUsers);
        res.put("activeUsers", activeUsers);
        res.put("totalSessions", totalSessions);
        res.put("completedSessions", completedSessions);
        res.put("totalStudyHours", totalStudyHours);
        res.put("avgStudyPerUser", avgStudyPerUser);
        return res;
    }

    public List<Map<String, Object>> getUsersStats() {
        List<Utilisateur> users = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.USER).toList();
        List<StudySession> sessions = sessionRepository.findAll();

        return users.stream().map(user -> {
            List<StudySession> userSessions = sessions.stream()
                    .filter(s -> s.getUserId().equals(user.getId())).toList();
            long completed = userSessions.stream()
                    .filter(s -> s.getStatus() == SessionStatus.DONE).count();
            long hours = userSessions.stream()
                    .filter(s -> s.getStatus() == SessionStatus.DONE)
                    .mapToLong(this::hoursOf).sum();

            Map<String, Object> map = new HashMap<>();
            map.put("userId", user.getId());
            map.put("username", user.getUsername());
            map.put("sessions", userSessions.size());
            map.put("completedSessions", completed);
            map.put("studyHours", hours);
            return map;
        }).toList();
    }

    // SUBJECTS GLOBAL POPULARITY — admin view
    public List<Map<String, Object>> getAdminSubjectsStats() {

        List<Subject> allSubjects = subjectRepository.findAll();
        List<StudySession> allSessions = sessionRepository.findAll();

        Map<String, Long> hoursPerSubject = allSessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.DONE)
                .filter(s -> s.getSubjectId() != null)
                .collect(Collectors.groupingBy(
                        StudySession::getSubjectId,
                        Collectors.summingLong(this::hoursOf)
                ));

        Map<String, Long> sessionsPerSubject = allSessions.stream()
                .filter(s -> s.getSubjectId() != null)
                .collect(Collectors.groupingBy(
                        StudySession::getSubjectId,
                        Collectors.counting()
                ));

        return allSubjects.stream()
                .map(subject -> {
                    long hours    = hoursPerSubject.getOrDefault(subject.getId(), 0L);
                    long sessions = sessionsPerSubject.getOrDefault(subject.getId(), 0L);

                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("subject",       subject.getName());
                    map.put("totalHours",    hours);
                    map.put("totalSessions", sessions);
                    return map;
                })
                .filter(m -> (Long) m.get("totalHours") > 0)
                .sorted(Comparator.comparingLong(
                        (Map<String, Object> m) -> (Long) m.get("totalHours")).reversed()
                )
                .toList();
    }

    public List<Map<String, Object>> getWeeklyTrend() {
        List<StudySession> sessions = sessionRepository.findAll();
        Map<String, long[]> map = new TreeMap<>();

        for (StudySession s : sessions) {
            if (s.getStartTime() == null) continue;
            String week = getWeekLabel(s.getStartTime().toLocalDate());
            map.computeIfAbsent(week, k -> new long[]{0, 0});
            map.get(week)[0]++;
            if (s.getStatus() == SessionStatus.DONE) map.get(week)[1] += hoursOf(s);
        }

        return map.entrySet().stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("week", e.getKey());
            m.put("sessions", e.getValue()[0]);
            m.put("hours", e.getValue()[1]);
            return m;
        }).toList();
    }

    public List<Map<String, Object>> getAllUsers() {
        List<Utilisateur> allUsers = userRepository.findAll();
        List<StudySession> allSessions = sessionRepository.findAll();

        return allUsers.stream().map(user -> {
            List<StudySession> userSessions = allSessions.stream()
                    .filter(s -> s.getUserId().equals(user.getId())).toList();
            long completed = userSessions.stream()
                    .filter(s -> s.getStatus() == SessionStatus.DONE).count();
            long hours = userSessions.stream()
                    .filter(s -> s.getStatus() == SessionStatus.DONE)
                    .mapToLong(this::hoursOf).sum();

            LocalDateTime limit = LocalDateTime.now().minusDays(7);
            boolean isActive = userSessions.stream()
                    .anyMatch(s -> s.getStartTime() != null && s.getStartTime().isAfter(limit));

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id",                user.getId());
            map.put("name",              user.getName());
            map.put("username",          user.getUsername());
            map.put("email",             user.getEmail());
            map.put("role",              user.getRole().name());
            map.put("sessions",          userSessions.size());
            map.put("completedSessions", completed);
            map.put("studyHours",        hours);
            map.put("active",            isActive);
            return map;
        }).toList();
    }

    public List<Map<String, Object>> getAllGroupsForAdmin() {
        return groupRepository.findAll().stream().map(group -> {
            String ownerUsername = userRepository.findById(group.getOwnerId())
                    .map(Utilisateur::getUsername).orElse("—");
            String ownerEmail = userRepository.findById(group.getOwnerId())
                    .map(Utilisateur::getEmail).orElse("—");

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id",           group.getId());
            map.put("name",         group.getName());
            map.put("ownerUsername",ownerUsername);
            map.put("ownerEmail",   ownerEmail);
            map.put("memberCount",  group.getMemberIds() != null ? group.getMemberIds().size() : 0);
            map.put("createdAt",    group.getCreatedAt());
            return map;
        }).toList();
    }
}