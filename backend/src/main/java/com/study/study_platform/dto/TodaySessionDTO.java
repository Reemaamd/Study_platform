package com.study.study_platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TodaySessionDTO {
    private String id;
    private String title;   // subject name
    private String start;
    private String end;
    private boolean active;
    private String color;
}
