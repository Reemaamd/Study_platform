package com.study.study_platform.dto;

import com.study.study_platform.model.document.Role;
import lombok.Data;

@Data
public class RegisterRequest {

    private String name;

    private String username;

    private String email;

    private String password;

    private Role role;
}