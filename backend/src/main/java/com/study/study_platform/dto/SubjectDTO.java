package com.study.study_platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubjectDTO {

    @NotBlank(message = "Name is required")
    private String name;

}