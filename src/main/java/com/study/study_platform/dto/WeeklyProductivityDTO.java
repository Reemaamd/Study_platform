package com.study.study_platform.dto;
import lombok.*;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyProductivityDTO {
    private String week;
    private long hoursStudied;
    private long sessionsCompleted;
    private long achievedObjectives;
}
