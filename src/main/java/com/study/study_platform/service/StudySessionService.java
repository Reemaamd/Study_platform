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
import java.util.*;

@Service
@RequiredArgsConstructor
public class StudySessionService {

    private final StudySessionRepository repository;
    private final UserRepository userRepository;
    private final ObjectiveRepository objectiveRepository;

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

        for (Objective obj : objectives) {

            LocalDate weekStart = obj.getWeekStartDate();
            LocalDate weekEnd = obj.getWeekEndDate();

            // ✅ IMPORTANT FIX: filter sessions by subject + week
            List<StudySession> existingSessions = repository.findByUserId(userId)
                    .stream()
                    .filter(s ->
                            s.getSubjectId().equals(obj.getSubjectId()) &&
                                    !s.getStartTime().toLocalDate().isBefore(weekStart) &&
                                    !s.getStartTime().toLocalDate().isAfter(weekEnd)
                    )
                    .toList();

            // ✅ calc remaining hours correctly
            int alreadyPlanned = existingSessions.stream()
                    .mapToInt(s -> (int) Duration.between(s.getStartTime(), s.getEndTime()).toHours())
                    .sum();

            int remaining = obj.getWeeklyGoal() - alreadyPlanned;

            if (remaining <= 0) continue;

            for (Availability av : user.getAvailabilities()) {

                DayOfWeek day = DayOfWeek.valueOf(av.getDay().name());

                LocalDate sessionDate = weekStart.with(day);

                if (sessionDate.isBefore(weekStart) || sessionDate.isAfter(weekEnd)) {
                    continue;
                }

                LocalTime startTime = LocalTime.parse(av.getStartTime());
                LocalTime endTime = LocalTime.parse(av.getEndTime());

                LocalDateTime start = sessionDate.atTime(startTime);
                LocalDateTime limit = sessionDate.atTime(endTime);

                while (remaining > 0 && start.isBefore(limit)) {

                    long duration = Math.min(MAX_HOURS_PER_SESSION, remaining);
                    LocalDateTime end = start.plusHours(duration);

                    if (end.isAfter(limit)) break;

                    if (!hasOverlap(userId, start, end, generatedSessions)) {

                        StudySession session = StudySession.builder()
                                .userId(userId)
                                .subjectId(obj.getSubjectId())
                                .startTime(start)
                                .endTime(end)
                                .status(SessionStatus.PLANNED)
                                .build();

                        StudySession saved = repository.save(session);
                        generatedSessions.add(saved);

                        remaining -= duration;
                    }

                    start = end;
                }

                if (remaining <= 0) break;
            }
        }

        return generatedSessions;
    }

    // ==============================
    // OVERLAP CHECK
    // ==============================
    private boolean hasOverlap(String userId,
                               LocalDateTime start,
                               LocalDateTime end,
                               List<StudySession> newSessions) {

        for (StudySession s : repository.findByUserId(userId)) {
            if (isOverlap(start, end, s.getStartTime(), s.getEndTime())) {
                return true;
            }
        }

        for (StudySession s : newSessions) {
            if (isOverlap(start, end, s.getStartTime(), s.getEndTime())) {
                return true;
            }
        }

        return false;
    }

    private boolean isOverlap(LocalDateTime s1, LocalDateTime e1,
                              LocalDateTime s2, LocalDateTime e2) {
        return s1.isBefore(e2) && e1.isAfter(s2);
    }

    // ==============================
    // COMPLETE SESSION
    // ==============================
    public StudySession completeSession(String sessionId) {

        StudySession session = repository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

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

        List<Objective> objectives = objectiveRepository.findByUserId(session.getUserId());

        Objective obj = objectives.stream()
                .filter(o ->
                        o.getSubjectId().equals(session.getSubjectId()) &&
                                !sessionDate.isBefore(o.getWeekStartDate()) &&
                                !sessionDate.isAfter(o.getWeekEndDate())
                )
                .findFirst()
                .orElse(null);

        if (obj == null) return;

        long hours = Duration.between(session.getStartTime(), session.getEndTime()).toHours();

        obj.setProgress(Math.min(
                obj.getProgress() + (int) hours,
                obj.getWeeklyGoal()
        ));

        objectiveRepository.save(obj);
    }
}