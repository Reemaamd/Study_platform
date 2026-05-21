package com.study.study_platform.service;

import com.study.study_platform.model.document.StudySession;
import com.study.study_platform.model.document.Subject;
import com.study.study_platform.repository.StudySessionRepository;
import com.study.study_platform.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionReminderService {

    private final StudySessionRepository sessionRepository;
    private final NotificationService notificationService;
    private final SubjectRepository subjectRepository;
    // 🔥 runs every 1 minute
    @Scheduled(fixedRate = 10000)
    public void sendSessionReminders() {


        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in30Min = now.plusMinutes(30);

        List<StudySession> sessions =
                sessionRepository.findByStartTimeBetween(now, in30Min);
        for (StudySession s : sessions) {

            if (s.getStartTime() == null) continue;

            long minutes = Duration.between(now, s.getStartTime()).toMinutes();

            String userId = s.getUserId();

            // 👉 récupérer subject UNE SEULE FOIS
            Subject subject = subjectRepository.findById(s.getSubjectId())
                    .orElse(null);

            String subjectName = (subject != null) ? subject.getName() : "Unknown subject";

            // ⏰ 15 minutes reminder
            if (minutes <= 15 && minutes > 14) {
                notificationService.send(
                        userId,
                        "⏰ Your session (" + subjectName + ") starts in 15 minutes",
                        "SESSION_REMINDER_15",
                        s.getId() + "_15"
                );
            }

            // ⏰ 5 minutes reminder
            if (minutes <= 5 && minutes > 4) {
                notificationService.send(
                        userId,
                        "⚠️ Your session (" + subjectName + ") starts in 5 minutes",
                        "SESSION_REMINDER_5",
                        s.getId() + "_5"
                );
            }
        }
    }
}