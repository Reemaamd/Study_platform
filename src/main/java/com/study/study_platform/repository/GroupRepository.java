package com.study.study_platform.repository;

import com.study.study_platform.model.document.Group;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GroupRepository extends MongoRepository<Group, String> {
}