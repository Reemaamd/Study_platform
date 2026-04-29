package com.study.study_platform.repository;

import com.study.study_platform.model.document.Subject;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SubjectRepository extends MongoRepository<Subject, String> {

    List<Subject> findByUserId(String userId);
    List<Subject> findByName(String name);
}