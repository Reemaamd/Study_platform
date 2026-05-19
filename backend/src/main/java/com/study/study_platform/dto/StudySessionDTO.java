package com.study.study_platform.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class StudySessionDTO {

    private String subjectId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String status;
}