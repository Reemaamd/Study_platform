package com.study.study_platform.repository;

import com.study.study_platform.model.document.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Notification> findByUserIdAndIsReadFalse(String userId);
    // ✅ AJOUT IMPORTANT (anti-doublon)
    boolean existsByUserIdAndTypeAndMessageAndExternalId(
            String userId,
            String type,
            String message,
            String externalId
    );

}