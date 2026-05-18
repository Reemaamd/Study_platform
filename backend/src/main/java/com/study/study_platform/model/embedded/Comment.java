package com.study.study_platform.model.embedded;

import lombok.*;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    private String userId;
    private String content;
    private Date timestamp;
}