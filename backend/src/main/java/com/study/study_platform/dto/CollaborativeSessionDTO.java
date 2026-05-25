package com.study.study_platform.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CollaborativeSessionDTO {

    private String groupId;

    private String subjectId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}