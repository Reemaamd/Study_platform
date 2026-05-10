package com.study.study_platform.dto;
import lombok.*;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubjectStatsDTO {
    private String subjectName;
    private long totalHours;
    private double progressPercentage;
}
