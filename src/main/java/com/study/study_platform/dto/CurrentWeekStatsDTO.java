package com.study.study_platform.dto;
import lombok.*;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentWeekStatsDTO {
    private long studyHours;
    private long completedSessions;
    private long achievedObjectives;
}