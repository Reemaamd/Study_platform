package com.study.study_platform.model.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "objectives")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Objective {

    @Id
    private String id;

    private String userId;
    private String subjectId;
    private String title; // ✅ AJOUT

    private int weeklyGoal;
    private int progress;
    private int priority;

    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
}