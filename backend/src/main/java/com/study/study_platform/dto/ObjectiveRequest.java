package com.study.study_platform.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.Date;

@Data
public class ObjectiveRequest {

    private String subjectId;
    private String title; // ✅ AJOUT
    private int weeklyGoal;
    private int priority;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;

}