package com.study.study_platform.model.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    private String id;

    private String senderId;
    private String groupId;

    private String content;

    private Date timestamp;
}