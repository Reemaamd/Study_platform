package com.study.study_platform.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class InvitationResponseDTO {

    private String id;

    private String senderId;

    private String receiverId;

    private String groupId;

    private String status;

    private Date createdAt;
}