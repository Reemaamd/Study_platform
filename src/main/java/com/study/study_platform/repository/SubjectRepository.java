package com.study.study_platform.repository;

import com.study.study_platform.model.document.Subject;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SubjectRepository extends MongoRepository<Subject, String> {
}