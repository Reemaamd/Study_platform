package com.study.study_platform.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GroupDTO {
    @NotBlank(message = "Group name is required")
    private String name;
}