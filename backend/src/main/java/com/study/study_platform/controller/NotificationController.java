package com.study.study_platform.controller;

import com.study.study_platform.model.document.Notification;
import com.study.study_platform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    // GET user notifications
    @GetMapping
    public List<Notification> getMyNotifications(
            @AuthenticationPrincipal UserDetails user
    ) {
        return service.getUserNotifications(user.getUsername());
    }

    // mark as read
    @PutMapping("/{id}/read")
    public void markAsRead(@PathVariable String id) {
        service.markAsRead(id);
    }

    // unread count
    @GetMapping("/unread-count")
    public long unreadCount(@AuthenticationPrincipal UserDetails user) {
        return service.unreadCount(user.getUsername());
    }
    // PUT /notifications/read-all
    @PutMapping("/mark-all-read")
    public void markAllAsRead(@RequestParam String username) {
        service.markAllAsRead(username);
    }
}