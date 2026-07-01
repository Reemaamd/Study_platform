package com.study.study_platform.service;

import com.study.study_platform.dto.OnboardingRequest;
import com.study.study_platform.dto.OnboardingSubjectDTO;
import com.study.study_platform.model.document.Objective;
import com.study.study_platform.model.document.Subject;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.model.embedded.Availability;
import java.time.DayOfWeek;
import com.study.study_platform.repository.ObjectiveRepository;
import com.study.study_platform.repository.SubjectRepository;
import com.study.study_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final ObjectiveRepository objectiveRepository;

    public void process(
            String username,
            OnboardingRequest request
    ) {

        Utilisateur user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found: " + username)
                );

        // ==============================
        // CURRENT WEEK DATES
        // ==============================
        LocalDate today = LocalDate.now();

        LocalDate weekStart = today.with(
                TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)
        );

        LocalDate weekEnd = today.with(
                TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY)
        );

        // ==============================
        // SAVE AVAILABILITIES
        // ==============================
        List<Availability> availabilities = request
                .getAvailability()
                .stream()
                .map(dto -> {

                    Availability availability = new Availability();

                    availability.setDay(
                            DayOfWeek.valueOf(dto.getDay())
                    );

                    availability.setStartTime(dto.getStartTime());

                    availability.setEndTime(dto.getEndTime());

                    return availability;
                })
                .toList();

        user.setAvailabilities(availabilities);

        userRepository.save(user);

        // ==============================
      // SAVE SUBJECTS + OBJECTIVES (si présents)
// ==============================
        if (request.getSubjects() != null) {

            for (OnboardingSubjectDTO dto : request.getSubjects()) {

                // ---------- SUBJECT ----------
                Subject subject = new Subject();
                subject.setName(dto.getName());
                subject.setUserId(user.getId());

                Subject savedSubject = subjectRepository.save(subject);

                // ---------- OBJECTIVE ----------
                Objective objective = new Objective();
                objective.setUserId(user.getId());
                objective.setSubjectId(savedSubject.getId());

                objective.setTitle(
                        dto.getTitle() != null && !dto.getTitle().isBlank()
                                ? dto.getTitle()
                                : dto.getName()
                );

                objective.setWeeklyGoal(dto.getWeeklyGoal());
                objective.setPriority(dto.getPriority() > 0 ? dto.getPriority() : 2);
                objective.setProgress(0);
                objective.setWeekStartDate(weekStart);
                objective.setWeekEndDate(weekEnd);

                objectiveRepository.save(objective);
            }
        }
    }
}