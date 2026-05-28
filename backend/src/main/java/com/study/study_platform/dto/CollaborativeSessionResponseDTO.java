package com.study.study_platform.dto;

import com.study.study_platform.model.enums.SessionStatus;
import com.study.study_platform.model.enums.SessionType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CollaborativeSessionResponseDTO {

    private String id;

    private String subjectId;

    private String groupId;

    private String userUsername;

    private String type;

    private String status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}