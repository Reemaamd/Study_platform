package com.study.study_platform.dto;

import lombok.Data;

import java.util.List;

@Data
public class OnboardingRequest {

    private List<OnboardingSubjectDTO> subjects;

    private List<AvailabilityDTO> availability;
}
