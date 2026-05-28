package com.study.study_platform.mapper;

import com.study.study_platform.dto.InvitationResponseDTO;
import com.study.study_platform.model.document.Group;
import com.study.study_platform.model.document.Invitation;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.repository.GroupRepository;
import com.study.study_platform.repository.UserRepository;

import org.springframework.stereotype.Component;

@Component
public class InvitationMapper {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    public InvitationMapper(
            UserRepository userRepository,
            GroupRepository groupRepository
    ) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
    }

    public InvitationResponseDTO toResponseDTO(
            Invitation invitation
    ) {

        Utilisateur sender = userRepository
                .findById(invitation.getSenderId())
                .orElse(null);

        Utilisateur receiver = userRepository
                .findById(invitation.getReceiverId())
                .orElse(null);

        Group group = groupRepository
                .findById(invitation.getGroupId())
                .orElse(null);

        return InvitationResponseDTO.builder()
                .id(invitation.getId())

                .senderId(invitation.getSenderId())
                .senderUsername(
                        sender != null
                                ? sender.getUsername()
                                : "Unknown"
                )

                .receiverId(invitation.getReceiverId())
                .receiverUsername(
                        receiver != null
                                ? receiver.getUsername()
                                : "Unknown"
                )

                .groupId(invitation.getGroupId())
                .groupName(
                        group != null
                                ? group.getName()
                                : "Unknown"
                )

                .status(invitation.getStatus())
                .createdAt(invitation.getCreatedAt())
                .build();
    }
}