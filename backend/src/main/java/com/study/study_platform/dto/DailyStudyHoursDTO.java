package com.study.study_platform.dto;
import lombok.*;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyStudyHoursDTO {
    private String day;
    private long hours;
}