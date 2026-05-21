package com.study.study_platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CommonAvailabilityDTO {

    private String day;

    private String startTime;

    private String endTime;
}