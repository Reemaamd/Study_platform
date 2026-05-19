package com.study.study_platform.repository;

import com.study.study_platform.model.document.Message;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MessageRepository extends MongoRepository<Message, String> {
}