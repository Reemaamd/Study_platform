package com.study.study_platform.dto;
import lombok.*;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private long totalObjectives;
    private long achievedObjectives;
    private long totalSessions;
    private long completedSessions;
    private long totalStudyHours;
    private double completionRate;
}