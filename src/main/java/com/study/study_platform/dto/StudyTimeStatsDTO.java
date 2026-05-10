package com.study.study_platform.dto;
import lombok.*;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudyTimeStatsDTO {
    private long plannedHours;
    private long completedHours;
    private long remainingHours;
}
