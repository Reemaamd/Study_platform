package com.study.study_platform.model.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "invitations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invitation {

    @Id
    private String id;

    private String senderId;
    private String receiverId;

    private String groupId;

    private String status; // PENDING / ACCEPTED / REJECTED

    private Date createdAt;
}
