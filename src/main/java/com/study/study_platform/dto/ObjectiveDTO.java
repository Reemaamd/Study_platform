package com.study.study_platform.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.Date;

@Data
public class ObjectiveDTO {

    private String id;
    private String subjectId;
    private String subjectName;
    private String title; // ✅ AJOUT
    private int weeklyGoal;
    private int progress;
    private int priority;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private int progressPercentage;
    private String status;
}