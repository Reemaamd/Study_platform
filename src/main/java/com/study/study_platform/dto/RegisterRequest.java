package com.study.study_platform.dto;

import com.study.study_platform.model.document.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    private String name;

    private String username;

    private String email;

    private String password;

    private Role role;
}