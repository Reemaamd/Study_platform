package com.study.study_platform.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class InvitationResponseDTO {

    private String id;

    private String senderId;
    private String senderUsername;

    private String receiverId;
    private String receiverUsername;

    private String groupId;
    private String groupName;

    private String status;

    private Date createdAt;
}