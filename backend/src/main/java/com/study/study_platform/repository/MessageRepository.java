package com.study.study_platform.repository;

import com.study.study_platform.model.document.Message;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends MongoRepository<Message, String> {

    List<Message> findByGroupIdOrderByCreatedAtAsc(String groupId);
    Optional<Message> findTopByGroupIdOrderByCreatedAtDesc(String groupId);
}