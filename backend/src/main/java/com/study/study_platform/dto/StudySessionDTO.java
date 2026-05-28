package com.study.study_platform.dto;

import com.study.study_platform.model.enums.SessionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class StudySessionDTO {
    private String id;
    private String subjectId;
    private String subjectName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private SessionStatus status;
}