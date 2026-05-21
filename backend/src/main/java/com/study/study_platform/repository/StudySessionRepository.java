package com.study.study_platform.repository;

import com.study.study_platform.model.document.StudySession;
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

}