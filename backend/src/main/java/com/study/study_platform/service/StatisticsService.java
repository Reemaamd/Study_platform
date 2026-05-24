package com.study.study_platform.service;

import com.study.study_platform.dto.*;
import com.study.study_platform.model.document.*;
import com.study.study_platform.model.enums.SessionStatus;
import com.study.study_platform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.IsoFields;
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
    // HELPER — récupérer user connecté
    // ─────────────────────────────────────────
    private Utilisateur getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ─────────────────────────────────────────
    // HELPER — label semaine
    // ─────────────────────────────────────────
    private String getWeekLabel(LocalDate date) {
        int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int year = date.get(IsoFields.WEEK_BASED_YEAR);
        return year + "-W" + String.format("%02d", week);
    }

    // ─────────────────────────────────────────
    // HELPER — durée en heures (Calculer durée d’une session en heures)
    // ─────────────────────────────────────────
    private long hoursOf(StudySession s) {

        if (s.getStartTime() == null || s.getEndTime() == null) {
            return 0;
        }

        return Duration.between(s.getStartTime(), s.getEndTime()).toHours();
    }

    // ─────────────────────────────────────────
    // 1. DASHBOARD GLOBAL
    // ─────────────────────────────────────────
    public DashboardStatsDTO getDashboard(String username) {

        String userId = getUser(username).getId();

        List<StudySession> sessions = sessionRepository.findByUserId(userId);
        sessions.forEach(s ->
                System.out.println(
                        "ID = " + s.getId()
                                + " | status = " + s.getStatus()
                                + " | user = " + s.getUserId()
                )
        );
        List<Objective> objectives = objectiveRepository.findByUserId(userId);

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
    // 2. STUDY TIME
    // ─────────────────────────────────────────
    public StudyTimeStatsDTO getStudyTime(String username) {

        String userId = getUser(username).getId();
        List<StudySession> sessions = sessionRepository.findByUserId(userId);

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
    // 3. SESSION PROGRESS
    // ─────────────────────────────────────────
    public SessionProgressDTO getSessionProgress(String username) {

        String userId = getUser(username).getId();
        List<StudySession> sessions = sessionRepository.findByUserId(userId);

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
    // 4. WEEKLY PRODUCTIVITY
    // ─────────────────────────────────────────
    public List<WeeklyProductivityDTO> getWeeklyProductivity(String username) {

        String userId = getUser(username).getId();
        List<StudySession> sessions = sessionRepository.findByUserId(userId);
        List<Objective> objectives = objectiveRepository.findByUserId(userId);

        // grouper sessions DONE par semaine
        Map<String, long[]> weekMap = new TreeMap<>();
        // [0] = heures, [1] = sessions done

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
                    .filter(o ->
                            o.getWeekStartDate() != null &&
                                    getWeekLabel(o.getWeekStartDate()).equals(week) &&
                                    o.getProgress() >= o.getWeeklyGoal())
                    .count();

            return new WeeklyProductivityDTO(
                    week,
                    e.getValue()[0],
                    e.getValue()[1],
                    achievedObj
            );
        }).toList();
    }

    // ─────────────────────────────────────────
    // 5. SUBJECT STATS (distribution + performance)
    // ─────────────────────────────────────────
    public List<SubjectStatsDTO> getSubjectStats(String username) {

        String userId = getUser(username).getId();
        List<StudySession> sessions = sessionRepository.findByUserId(userId);
        List<Objective> objectives = objectiveRepository.findByUserId(userId);

        // heures DONE par subjectId
        Map<String, Long> hoursPerSubject = sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.DONE)
                .collect(Collectors.groupingBy(
                        StudySession::getSubjectId,
                        Collectors.summingLong(this::hoursOf)
                ));

        return hoursPerSubject.entrySet().stream().map(e -> {

                    String subjectId = e.getKey();
                    long hours = e.getValue();

                    String name = subjectRepository.findById(subjectId)
                            .map(Subject::getName)
                            .orElse("Unknown");

                    // objectif lié → goal total pour cette matière
                    int totalGoal = objectives.stream()
                            .filter(o -> o.getSubjectId().equals(subjectId))
                            .mapToInt(Objective::getWeeklyGoal)
                            .sum();

                    double progress = totalGoal == 0 ? 100.0 :
                            (double) hours / totalGoal * 100;

                    return new SubjectStatsDTO(name, hours, Math.min(progress, 100.0));

                }).sorted(Comparator.comparingLong(SubjectStatsDTO::getTotalHours).reversed())
                .toList();
    }

    // ─────────────────────────────────────────
    // 6. DAILY HOURS
    // ─────────────────────────────────────────
    public List<DailyStudyHoursDTO> getDailyHours(String username) {

        String userId = getUser(username).getId();

        Map<String, Long> map = sessionRepository.findByUserId(userId).stream()
                .filter(s -> s.getStatus() == SessionStatus.DONE)
                .collect(Collectors.groupingBy(
                        s -> s.getStartTime().getDayOfWeek().name(),
                        Collectors.summingLong(this::hoursOf)
                ));

        // ordre logique lundi → dimanche
        List<String> order = List.of(
                "MONDAY","TUESDAY","WEDNESDAY",
                "THURSDAY","FRIDAY","SATURDAY","SUNDAY"
        );

        return order.stream()
                .map(day -> new DailyStudyHoursDTO(
                        day,
                        map.getOrDefault(day, 0L)
                ))
                .toList();
    }

    // ─────────────────────────────────────────
    // 7. OBJECTIVE COMPLETION
    // ─────────────────────────────────────────
    public ObjectiveCompletionDTO getObjectiveCompletion(String username) {

        String userId = getUser(username).getId();
        List<Objective> objectives = objectiveRepository.findByUserId(userId);

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
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd   = today.with(DayOfWeek.SUNDAY);

        List<StudySession> sessions = sessionRepository.findByUserId(userId).stream()
                .filter(s -> {
                    LocalDate d = s.getStartTime().toLocalDate();
                    return !d.isBefore(weekStart) && !d.isAfter(weekEnd);
                }).toList();

        long hours = sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.DONE)
                .mapToLong(this::hoursOf)
                .sum();

        long completedSessions = sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.DONE)
                .count();

        long achievedObjectives = objectiveRepository.findByUserId(userId).stream()
                .filter(o -> o.getWeekStartDate() != null &&
                        !o.getWeekStartDate().isBefore(weekStart) &&
                        !o.getWeekStartDate().isAfter(weekEnd) &&
                        o.getProgress() >= o.getWeeklyGoal())
                .count();

        return new CurrentWeekStatsDTO(hours, completedSessions, achievedObjectives);
    }
//📊 GLOBAL PLATFORM DASHBOARD
    public Map<String, Object> getAdminDashboard() {

        List<Utilisateur> users = userRepository.findAll()
                .stream()
                .filter(u -> u.getRole() == Role.USER).toList();
        List<StudySession> sessions = sessionRepository.findAll();

        long totalUsers = users.size();
        long totalSessions = sessions.size();

        long completedSessions = sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.DONE)
                .count();

        long totalStudyHours = sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.DONE)
                .mapToLong(this::hoursOf)
                .sum();

        // ACTIVE USERS = sessions last 7 days
        LocalDateTime limit = LocalDateTime.now().minusDays(7);

        long activeUsers = sessions.stream()
                .filter(s -> s.getStartTime() != null)
                .filter(s -> s.getStartTime().isAfter(limit))
                .map(StudySession::getUserId)
                .distinct()
                .count();

        double avgStudyPerUser = totalUsers == 0 ? 0 :
                (double) totalStudyHours / totalUsers;

        Map<String, Object> res = new HashMap<>();
        res.put("totalUsers", totalUsers);
        res.put("activeUsers", activeUsers);
        res.put("totalSessions", totalSessions);
        res.put("completedSessions", completedSessions);
        res.put("totalStudyHours", totalStudyHours);
        res.put("avgStudyPerUser", avgStudyPerUser);

        return res;
    }
//USERS ANALYTICS
    public List<Map<String, Object>> getUsersStats() {

        List<Utilisateur> users = userRepository.findAll()
                .stream()
                .filter(u -> u.getRole() == Role.USER).toList();
        List<StudySession> sessions = sessionRepository.findAll();

        return users.stream().map(user -> {

            List<StudySession> userSessions = sessions.stream()
                    .filter(s -> s.getUserId().equals(user.getId()))
                    .toList();

            long completed = userSessions.stream()
                    .filter(s -> s.getStatus() == SessionStatus.DONE)
                    .count();

            long hours = userSessions.stream()
                    .filter(s -> s.getStatus() == SessionStatus.DONE)
                    .mapToLong(this::hoursOf)
                    .sum();

            Map<String, Object> map = new HashMap<>();
            map.put("userId", user.getId());
            map.put("username", user.getUsername());
            map.put("sessions", userSessions.size());
            map.put("completedSessions", completed);
            map.put("studyHours", hours);

            return map;

        }).toList();
    }
//SUBJECTS GLOBAL POPULARITY
    public List<Map<String, Object>> getSubjectsStats() {

        List<Subject> subjects = subjectRepository.findAll();
        List<StudySession> sessions = sessionRepository.findAll();

        return subjects.stream().map(subject -> {

            List<StudySession> subjectSessions = sessions.stream()
                    .filter(s -> s.getSubjectId().equals(subject.getId()))
                    .toList();

            long totalHours = subjectSessions.stream()
                    .filter(s -> s.getStatus() == SessionStatus.DONE)
                    .mapToLong(this::hoursOf)
                    .sum();

            Map<String, Object> map = new HashMap<>();
            map.put("subject", subject.getName());
            map.put("totalSessions", subjectSessions.size());
            map.put("totalHours", totalHours);

            return map;

        }).toList();
    }
//📈 WEEKLY PLATFORM TREND
    public List<Map<String, Object>> getWeeklyTrend() {

        List<StudySession> sessions = sessionRepository.findAll();

        Map<String, long[]> map = new TreeMap<>();

        for (StudySession s : sessions) {

            if (s.getStartTime() == null) continue;

            String week = getWeekLabel(s.getStartTime().toLocalDate());

            map.computeIfAbsent(week, k -> new long[]{0, 0});

            map.get(week)[0]++; // sessions

            if (s.getStatus() == SessionStatus.DONE) {
                map.get(week)[1] += hoursOf(s);
            }
        }

        return map.entrySet().stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("week", e.getKey());
            m.put("sessions", e.getValue()[0]);
            m.put("hours", e.getValue()[1]);
            return m;
        }).toList();
    }
    // ══════════════════════════════════════════════════════════════
// AJOUTS dans AdminService (ou StatisticsService selon ton archi)
// ══════════════════════════════════════════════════════════════

    // 1) Tous les users (USER + ADMIN) avec leurs stats
    public List<Map<String, Object>> getAllUsers() {
        List<Utilisateur> allUsers = userRepository.findAll();
        List<StudySession> allSessions = sessionRepository.findAll();

        return allUsers.stream().map(user -> {
            List<StudySession> userSessions = allSessions.stream()
                    .filter(s -> s.getUserId().equals(user.getId()))
                    .toList();

            long completed = userSessions.stream()
                    .filter(s -> s.getStatus() == SessionStatus.DONE)
                    .count();

            long hours = userSessions.stream()
                    .filter(s -> s.getStatus() == SessionStatus.DONE)
                    .mapToLong(this::hoursOf)
                    .sum();

            // Statut : actif si session dans les 7 derniers jours
            LocalDateTime limit = LocalDateTime.now().minusDays(7);
            boolean isActive = userSessions.stream()
                    .anyMatch(s -> s.getStartTime() != null
                            && s.getStartTime().isAfter(limit));

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id",               user.getId());
            map.put("name",             user.getName());
            map.put("username",         user.getUsername());
            map.put("email",            user.getEmail());
            map.put("role",             user.getRole().name()); // "USER" ou "ADMIN"
            map.put("sessions",         userSessions.size());
            map.put("completedSessions",completed);
            map.put("studyHours",       hours);
            map.put("active",           isActive);
            return map;
        }).toList();
    }

    // 2) Tous les groupes avec username du propriétaire
    public List<Map<String, Object>> getAllGroupsForAdmin() {
        List<Group> groups = groupRepository.findAll();

        return groups.stream().map(group -> {
            // Résoudre le username du propriétaire depuis ownerId
            String ownerUsername = userRepository.findById(group.getOwnerId())
                    .map(Utilisateur::getUsername)
                    .orElse("—");

            String ownerEmail = userRepository.findById(group.getOwnerId())
                    .map(Utilisateur::getEmail)
                    .orElse("—");

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id",           group.getId());
            map.put("name",         group.getName());
            //map.put("description",  group.getDescription());
            map.put("ownerUsername",ownerUsername);
            map.put("ownerEmail",   ownerEmail);
            map.put("memberCount",  group.getMemberIds() != null ? group.getMemberIds().size() : 0);
            map.put("createdAt",    group.getCreatedAt());
            return map;
        }).toList();
    }
}