package com.study.study_platform.dto;
import lombok.*;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObjectiveCompletionDTO {
    private long totalObjectives;
    private long achievedObjectives;
    private double completionRate;
}