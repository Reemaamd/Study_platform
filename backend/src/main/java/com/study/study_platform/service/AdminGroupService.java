package com.study.study_platform.service;

import com.study.study_platform.dto.AdminGroupDTO;
import com.study.study_platform.model.document.Group;
import com.study.study_platform.model.document.Message;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.repository.GroupRepository;
import com.study.study_platform.repository.MessageRepository;
import com.study.study_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminGroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    public List<AdminGroupDTO> getAllGroups() {

        List<Group> groups = groupRepository.findAll();

        return groups.stream().map(group -> {

            // OWNER
            Utilisateur owner = userRepository.findById(group.getOwnerId())
                    .orElse(null);

            // LAST MESSAGE
            Message lastMessage = messageRepository
                    .findTopByGroupIdOrderByCreatedAtDesc(group.getId())
                    .orElse(null);

            String status = "ACTIVE";

            if (lastMessage != null && lastMessage.getCreatedAt() != null) {
                long hours = Duration.between(
                        lastMessage.getCreatedAt(),
                        LocalDateTime.now(java.time.ZoneOffset.UTC)
                ).toHours();

                if (hours <= 24 * 7) { // 7 jours
                    status = "ACTIVE";
                }else { status = "INACTIVE";}
            }

            return AdminGroupDTO.builder()
                    .id(group.getId())
                    .name(group.getName())
                    .ownerUsername(owner != null ? owner.getUsername() : "unknown")
                    .ownerEmail(owner != null ? owner.getEmail() : "unknown")
                    .memberCount(group.getMemberIds() != null ? group.getMemberIds().size() : 0)
                    .createdAt(group.getCreatedAt())
                    .status(status)
                    .build();
        }).toList();
    }
}