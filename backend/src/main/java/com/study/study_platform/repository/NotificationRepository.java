package com.study.study_platform.repository;

import com.study.study_platform.model.document.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationRepository extends MongoRepository<Notification, String> {
}