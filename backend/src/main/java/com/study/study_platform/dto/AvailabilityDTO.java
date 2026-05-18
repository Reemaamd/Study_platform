package com.study.study_platform.dto;

import lombok.*;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityDTO {

    private String day;

    private String startTime;

    private String endTime;
}