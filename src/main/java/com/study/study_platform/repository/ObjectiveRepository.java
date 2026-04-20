package com.study.study_platform.repository;

import com.study.study_platform.model.document.Objective;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ObjectiveRepository extends MongoRepository<Objective, String> {
}