package com.study.study_platform.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class GroupResponseDTO {

    private String id;

    private String name;

    private String ownerId;

    private String ownerUsername;

    private List<MemberDTO> members;

    private LocalDateTime createdAt;
}