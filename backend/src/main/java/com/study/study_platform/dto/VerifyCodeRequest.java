// VerifyCodeRequest.java
package com.study.study_platform.dto;
import lombok.Data;

@Data
public class VerifyCodeRequest {
    private String email;
    private String code;
}