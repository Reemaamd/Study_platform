package com.study.study_platform.model.document;

import com.study.study_platform.model.embedded.Comment;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document(collection = "study_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudySession {

    @Id
    private String id;

    private Date startTime;
    private Date endTime;

    private String status;

    private String userId;
    private String subjectId;
    private String groupId;

    private List<Comment> comments;
}