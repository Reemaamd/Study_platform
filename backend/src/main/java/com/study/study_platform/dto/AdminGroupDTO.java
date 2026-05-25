package com.study.study_platform.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminGroupDTO {

    private String id;
    private String name;

    private String ownerUsername;
    private String ownerEmail;

    private int memberCount;

    private LocalDateTime createdAt;

    private String status; // ACTIVE / INACTIVE
}
