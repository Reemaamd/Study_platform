package com.study.study_platform.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberDTO {

    private String id;

    private String username;

    private boolean owner;
}