package com.study.study_platform.service;

import com.study.study_platform.model.document.Notification;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.repository.NotificationRepository;
import com.study.study_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final UserRepository userRepository;
    private final NotificationRepository repository;

    // =========================
    // CREATE NOTIFICATION
    // =========================
    public Notification send(String userId, String message, String type, String externalId) {

        boolean exists = repository.existsByUserIdAndTypeAndMessageAndExternalId(
                userId, type, message, externalId
        );

        if (exists) return null;

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setMessage(message);
        notification.setType(type);
        notification.setExternalId(externalId);
        notification.setRead(false);
        notification.setCreatedAt(new Date());

        return repository.save(notification);
    }

    // =========================
    // GET USER NOTIFICATIONS
    // =========================
    public List<Notification> getUserNotifications(String username) {

        Utilisateur user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return repository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    // =========================
    // MARK AS READ
    // =========================
    public void markAsRead(String id) {

        Notification n = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        n.setRead(true);
        repository.save(n);
    }

    // =========================
    // UNREAD COUNT
    // =========================
    public long unreadCount(String username) {

        Utilisateur user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return repository.findByUserIdAndIsReadFalse(user.getId()).size();
    }
}
