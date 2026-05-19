package com.study.study_platform.repository;

import com.study.study_platform.model.document.Group;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends MongoRepository<Group, String> {

    Optional<Group> findByName(String name);

    boolean existsByName(String name);

    List<Group> findByMemberIdsContaining(String userId);
}