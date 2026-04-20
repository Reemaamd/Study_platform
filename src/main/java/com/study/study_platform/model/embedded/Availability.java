package com.study.study_platform.model.embedded;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Availability {

    private String day;
    private String startTime;
    private String endTime;
}