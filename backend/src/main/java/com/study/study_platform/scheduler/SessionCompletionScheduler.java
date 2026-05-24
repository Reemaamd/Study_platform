package com.study.study_platform.scheduler;

import com.study.study_platform.model.document.StudySession;
import com.study.study_platform.model.enums.SessionStatus;
import com.study.study_platform.repository.StudySessionRepository;
import com.study.study_platform.service.StudySessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionCompletionScheduler {

    private final StudySessionRepository sessionRepository;
    private final StudySessionService sessionService;

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void updateSessionStatuses() {

        LocalDateTime now = LocalDateTime.now();

        // 1. PLANNED → ONGOING
        List<StudySession> toStart = sessionRepository
                .findByStatusAndStartTimeBefore(
                        SessionStatus.PLANNED,
                        now
                );

        for (StudySession s : toStart) {
            s.setStatus(SessionStatus.ONGOING);
        }

        // 2. ONGOING → DONE
        List<StudySession> toFinish = sessionRepository
                .findByStatusAndEndTimeBefore(
                        SessionStatus.ONGOING,
                        now
                );

        for (StudySession s : toFinish) {
            s.setStatus(SessionStatus.DONE);
            sessionService.updateObjectiveProgress(s);
        }

        sessionRepository.saveAll(toStart);
        sessionRepository.saveAll(toFinish);

        log.info("[Scheduler] updated: {} started, {} finished",
                toStart.size(), toFinish.size());
    }
}