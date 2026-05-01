package com.study.study_platform.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubjectResponseDTO {

    private String id;
    private String name;           // nom du subject

    private String username;       // login user
    private String userFullName;   // name
    private String email;          // email
}