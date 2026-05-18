package com.study.study_platform.model.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Group {

    @Id
    private String id;

    private String name;

    private String ownerId; // ✅ ajouté

    private List<String> memberIds;
}
