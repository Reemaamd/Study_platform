package com.study.study_platform.repository;

import com.study.study_platform.model.document.StudySession;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StudySessionRepository extends MongoRepository<StudySession, String> {
}