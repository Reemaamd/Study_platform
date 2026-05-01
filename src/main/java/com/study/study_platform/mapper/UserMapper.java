package com.study.study_platform.mapper;

import com.study.study_platform.dto.UserRequest;
import com.study.study_platform.dto.UserResponse;
import com.study.study_platform.model.document.Utilisateur;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public Utilisateur toEntity(UserRequest request) {
        return Utilisateur.builder()
                .name(request.getName())
                .username(request.getUsername())
                .role(request.getRole())
                .build();
    }

    public UserResponse toResponse(Utilisateur user) {
        return UserResponse.builder()
                .name(user.getName())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    public void updateEntity(Utilisateur user, UserRequest request) {

        if (request.getName() != null) {
            user.setName(request.getName());
        }

        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
    }
}