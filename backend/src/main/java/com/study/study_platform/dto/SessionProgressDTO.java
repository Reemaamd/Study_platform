package com.study.study_platform.dto;
import lombok.*;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionProgressDTO {
    private long plannedSessions;
    private long completedSessions;
    private double completionRate;
}
