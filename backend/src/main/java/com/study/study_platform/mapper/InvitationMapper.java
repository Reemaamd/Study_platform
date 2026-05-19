package com.study.study_platform.mapper;

import com.study.study_platform.dto.InvitationResponseDTO;
import com.study.study_platform.model.document.Invitation;
import org.springframework.stereotype.Component;

@Component
public class InvitationMapper {

    public InvitationResponseDTO toResponseDTO(Invitation invitation) {

        return InvitationResponseDTO.builder()
                .id(invitation.getId())
                .senderId(invitation.getSenderId())
                .receiverId(invitation.getReceiverId())
                .groupId(invitation.getGroupId())
                .status(invitation.getStatus())
                .createdAt(invitation.getCreatedAt())
                .build();
    }
}