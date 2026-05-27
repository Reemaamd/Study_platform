package com.study.study_platform.service;

import com.study.study_platform.dto.StudySessionDTO;
import com.study.study_platform.model.document.Objective;
import com.study.study_platform.model.document.StudySession;
import com.study.study_platform.model.document.Subject;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.model.embedded.Availability;
import com.study.study_platform.model.enums.SessionStatus;
import com.study.study_platform.repository.ObjectiveRepository;
import com.study.study_platform.repository.StudySessionRepository;
import com.study.study_platform.repository.SubjectRepository;
import com.study.study_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudySessionService {

    private final StudySessionRepository repository;
    private final UserRepository userRepository;
    private final ObjectiveRepository objectiveRepository;
    private final NotificationService notificationService;
    private final SubjectRepository subjectRepository;


    private static final int MAX_HOURS_PER_SESSION = 3;

    // ==============================
    // GENERATE WEEKLY PLAN
    // ==============================
    public List<StudySession> generateWeeklyPlan(String username) {

        Utilisateur user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        System.out.println("GENERATE FOR USER = " + username);

        if (user.getAvailabilities() == null || user.getAvailabilities().isEmpty()) {
            throw new RuntimeException("No availability defined");
        }

        String userId = user.getId();
        LocalDate today = LocalDate.now();
        LocalDate currentWeekStart = today.with(DayOfWeek.MONDAY);
        LocalDate currentWeekEnd   = today.with(DayOfWeek.SUNDAY);

        // ==============================
        // LOAD CURRENT WEEK SESSIONS
        // ==============================
        List<StudySession> allSessions = repository.findByUserId(userId)
                .stream()
                .filter(s -> {
                    LocalDate d = s.getStartTime().toLocalDate();
                    return !d.isBefore(currentWeekStart)
                            && !d.isAfter(currentWeekEnd);
                })
                .collect(Collectors.toCollection(ArrayList::new));

        // ==============================
        // DELETE PLANNED → regenerate cleanly
        // DONE sessions preserved
        // ==============================
        List<StudySession> plannedToDelete = allSessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.PLANNED)
                .toList();

        repository.deleteAll(plannedToDelete);
        allSessions.removeAll(plannedToDelete);

        // ==============================
        // LOAD OBJECTIVES
        // ==============================
        List<Objective> objectives = objectiveRepository.findByUserId(userId)
                .stream()
                .filter(o ->
                        o.getWeekStartDate() != null &&
                                o.getWeekEndDate() != null &&
                                !o.getWeekEndDate().isBefore(today)
                )
                .sorted(Comparator.comparingInt(Objective::getPriority).reversed())
                .toList();

        List<StudySession> generatedSessions = new ArrayList<>();

        // ==============================
        // GENERATION LOOP
        // ==============================
        for (Objective obj : objectives) {

            // ==============================
            // DONE hours this week for this subject
            // ==============================
            int doneHours = allSessions.stream()
                    .filter(s -> s.getSubjectId().equals(obj.getSubjectId()))
                    .filter(s -> {
                        LocalDate d = s.getStartTime().toLocalDate();
                        return !d.isBefore(currentWeekStart)
                                && !d.isAfter(currentWeekEnd);
                    })
                    .mapToInt(s ->
                            (int) Duration.between(
                                    s.getStartTime(),
                                    s.getEndTime()
                            ).toHours()
                    )
                    .sum();

            int remaining = obj.getWeeklyGoal() - obj.getProgress();

            if (remaining <= 0) continue;

            // ==============================
            // SORT AVAILABILITIES BY DAY ORDER
            // ==============================
            List<Availability> sortedAvailabilities = user.getAvailabilities()
                    .stream()
                    .sorted(Comparator.comparingInt(av ->
                            DayOfWeek.valueOf(av.getDay().name()).getValue()
                    ))
                    .toList();

            for (Availability av : sortedAvailabilities) {

                if (remaining <= 0) break;

                DayOfWeek targetDay = DayOfWeek.valueOf(av.getDay().name());

                // find the date of this day in current week
                LocalDate sessionDate = currentWeekStart
                        .with(TemporalAdjusters.nextOrSame(targetDay));

                if (sessionDate.isAfter(currentWeekEnd)) continue;
                if (sessionDate.isBefore(today)) continue;

                LocalDateTime start = sessionDate.atTime(
                        LocalTime.parse(av.getStartTime()));
                LocalDateTime limit = sessionDate.atTime(
                        LocalTime.parse(av.getEndTime()));

                // ✅ Si c'est aujourd'hui, ajuster le start à maintenant si la dispo a déjà commencé
                if (sessionDate.isEqual(today)) {
                    LocalDateTime now = LocalDateTime.now();
                    if (now.isAfter(limit)) continue;        // toute la plage est passée → skip
                    if (now.isAfter(start)) start = now;     // plage partiellement passée → ajuster
                }

                while (remaining > 0 && start.isBefore(limit)) {

                    long maxPossible = Duration.between(start, limit).toHours();
                    if (maxPossible <= 0) break;

                    long duration = Math.min(
                            Math.min(MAX_HOURS_PER_SESSION, remaining),
                            maxPossible
                    );

                    LocalDateTime end = start.plusHours(duration);

                    if (!hasOverlap(start, end, allSessions)) {

                        StudySession session = StudySession.builder()
                                .userId(userId)
                                .subjectId(obj.getSubjectId())
                                .startTime(start)
                                .endTime(end)
                                .status(SessionStatus.PLANNED)
                                .build();

                        generatedSessions.add(session);
                        allSessions.add(session);
                        remaining -= (int) duration;
                    }

                    start = end;
                }
            }
            System.out.println("GENERATE FOR USER = " + username);
        }
        notificationService.send(
                userId,
                "📅 Your weekly study plan has been generated successfully",
                "PLANNING_GENERATED",
                "plan_" + userId + "_" + LocalDate.now()
        );

        // ==============================
        // SAVE ALL AT ONCE
        // ==============================
        return repository.saveAll(generatedSessions);
    }

    // ==============================
    // COMPLETE SESSION
    // ==============================
    public StudySession completeSession(String sessionId, String userId) {

        StudySession session = repository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        // seulement ONGOING peut être terminé
        if (session.getStatus() != SessionStatus.ONGOING) {
            throw new RuntimeException("Session must be ONGOING to complete");
        }

        session.setStatus(SessionStatus.DONE);
        repository.save(session);

        updateObjectiveProgress(session);

        return session;
    }
    public void updateSessionToOngoing(String userId) {

        LocalDateTime now = LocalDateTime.now();

        List<StudySession> sessions = repository.findByUserId(userId);

        List<StudySession> toUpdate = sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.PLANNED)
                .filter(s -> !now.isBefore(s.getStartTime()))
                .filter(s -> now.isBefore(s.getEndTime()))
                .toList();

        for (StudySession s : toUpdate) {
            s.setStatus(SessionStatus.ONGOING);
        }

        repository.saveAll(toUpdate);
    }

    // ==============================
    // UPDATE OBJECTIVE PROGRESS
    // ==============================
    public void updateObjectiveProgress(StudySession session) {

        LocalDate sessionDate = session.getStartTime().toLocalDate();

        Objective obj = objectiveRepository.findByUserId(session.getUserId())
                .stream()
                .filter(o ->
                        o.getSubjectId() != null && // ✅ évite le NullPointerException
                                o.getWeekStartDate() != null && // ✅ sécuriser aussi les dates
                                o.getWeekEndDate() != null &&
                                o.getSubjectId().equals(session.getSubjectId()) &&
                                !sessionDate.isBefore(o.getWeekStartDate()) &&
                                !sessionDate.isAfter(o.getWeekEndDate())
                )
                .findFirst()
                .orElse(null);

        if (obj == null) return;

        long hours = Duration.between(
                session.getStartTime(), session.getEndTime()).toHours();

        obj.setProgress(Math.min(
                obj.getProgress() + (int) hours,
                obj.getWeeklyGoal()
        ));

        objectiveRepository.save(obj);
    }

    // ==============================
    // OVERLAP CHECK
    // ==============================
    private boolean hasOverlap(LocalDateTime start,
                               LocalDateTime end,
                               List<StudySession> sessions) {

        return sessions.stream().anyMatch(s ->
                start.isBefore(s.getEndTime()) && end.isAfter(s.getStartTime())
        );
    }
    public int calculateFocusStreak(String userId) {


        List<StudySession> sessions = repository.findByUserId(userId)
                .stream()
                .filter(s -> s.getStatus() == SessionStatus.DONE)
                .toList();

        // extraire les dates uniques
        Set<LocalDate> studyDays = sessions.stream()
                .map(s -> s.getStartTime().toLocalDate())
                .collect(Collectors.toSet());

        LocalDate today = LocalDate.now();

        int streak = 0;
        LocalDate current = today;

        // si aujourd’hui pas de study → on commence hier
        if (!studyDays.contains(today)) {
            current = today.minusDays(1);
        }

        while (studyDays.contains(current)) {
            streak++;
            current = current.minusDays(1);
        }

        return streak;
    }

    public List<StudySessionDTO> getUserSessions(String username, String startDate, String endDate) {

        Utilisateur user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ✅ Utilise les dates du frontend, sinon semaine courante par défaut
        LocalDateTime weekStart = startDate != null
                ? LocalDate.parse(startDate).atStartOfDay()
                : LocalDate.now().with(java.time.DayOfWeek.MONDAY).atStartOfDay();

        LocalDateTime weekEnd = endDate != null
                ? LocalDate.parse(endDate).atTime(23, 59, 59)
                : LocalDate.now().with(java.time.DayOfWeek.SUNDAY).atTime(23, 59, 59);

        List<StudySession> sessions = repository.findByUserIdAndStartTimeBetween(
                user.getId(), weekStart, weekEnd
        );

        return sessions.stream().map(session -> {
            Subject subject = subjectRepository.findById(session.getSubjectId()).orElse(null);
            return StudySessionDTO.builder()
                    .id(session.getId())
                    .subjectId(session.getSubjectId())
                    .subjectName(subject != null ? subject.getName() : "Unknown")
                    .startTime(session.getStartTime())
                    .endTime(session.getEndTime())
                    .status(session.getStatus())
                    .build();
        }).toList();
    }
}