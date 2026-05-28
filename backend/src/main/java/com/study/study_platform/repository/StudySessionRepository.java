package com.study.study_platform.repository;

import com.study.study_platform.model.document.StudySession;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.model.enums.SessionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public interface StudySessionRepository extends MongoRepository<StudySession, String> {

    List<StudySession> findByUserId(String userId);
    List<StudySession> findByStartTimeBetween(
            LocalDateTime start,
            LocalDateTime end
    );
    List<StudySession> findByGroupIdOrderByStartTimeAsc(String groupId);

    List<StudySession> findByUserIdAndStartTimeBetween(
            String userId,
            LocalDateTime start,
            LocalDateTime end
    );
    List<StudySession> findByStatusInAndEndTimeBefore(
            List<SessionStatus> statuses,
            LocalDateTime endTime
    );
    List<StudySession> findByStatusAndStartTimeBefore(SessionStatus status, LocalDateTime time);

    List<StudySession> findByStatusAndEndTimeBefore(SessionStatus status, LocalDateTime time);
}