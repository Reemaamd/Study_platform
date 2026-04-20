package com.study.study_platform.model.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "objectives")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Objective {

    @Id
    private String id;

    private String userId;
    private String subjectId;

    private int weeklyGoal;
    private int progress;
    private int priority;

    private Date weekStartDate;
    private Date weekEndDate;
}