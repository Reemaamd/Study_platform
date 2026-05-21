package com.study.study_platform.service;

import com.study.study_platform.model.document.Objective;
import com.study.study_platform.model.document.StudySession;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.model.embedded.Availability;
import com.study.study_platform.model.enums.SessionStatus;
import com.study.study_platform.repository.ObjectiveRepository;
import com.study.study_platform.repository.StudySessionRepository;
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

    private static final int MAX_HOURS_PER_SESSION = 3;

    // ==============================
    // GENERATE WEEKLY PLAN
    // ==============================
    public List<StudySession> generateWeeklyPlan(String username) {

        Utilisateur user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

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

                LocalDateTime start = sessionDate.atTime(
                        LocalTime.parse(av.getStartTime()));
                LocalDateTime limit = sessionDate.atTime(
                        LocalTime.parse(av.getEndTime()));

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

        // 🔒 SECURITY CHECK
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (session.getStatus() == SessionStatus.DONE) {
            throw new RuntimeException("Session already completed");
        }

        if (session.getEndTime().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Session not finished yet");
        }

        session.setStatus(SessionStatus.DONE);
        repository.save(session);

        updateObjectiveProgress(session);

        return session;
    }

    // ==============================
    // UPDATE OBJECTIVE PROGRESS
    // ==============================
    private void updateObjectiveProgress(StudySession session) {

        LocalDate sessionDate = session.getStartTime().toLocalDate();

        Objective obj = objectiveRepository.findByUserId(session.getUserId())
                .stream()
                .filter(o ->
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
}