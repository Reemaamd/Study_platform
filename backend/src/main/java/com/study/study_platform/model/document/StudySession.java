package com.study.study_platform.model.document;

import com.study.study_platform.model.embedded.Comment;
import com.study.study_platform.model.enums.SessionStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document(collection = "study_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudySession {

    @Id
    private String id;

    private LocalDateTime startTime;
    private LocalDateTime endTime;


    private SessionStatus status;

    private String userId;
    private String subjectId;
    private String groupId;

    private List<Comment> comments = new ArrayList<>();
}