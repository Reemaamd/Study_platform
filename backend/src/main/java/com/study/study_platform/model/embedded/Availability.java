package com.study.study_platform.model.embedded;

import java.time.DayOfWeek;
import lombok.*;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Availability {

    private DayOfWeek day;

    private String startTime;
    private String endTime;
}