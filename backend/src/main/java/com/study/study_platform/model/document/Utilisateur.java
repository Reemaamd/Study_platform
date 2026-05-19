package com.study.study_platform.model.document;

import com.study.study_platform.model.embedded.Availability;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "utilisateurs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur {

    @Id
    private String id;
    private boolean passwordMustChange;  // <-- forcer changement mot de passe

    private String name;
    private String username;
    private String email;
    private String password;

    private Role role; // USER / ADMIN

    private List<Availability> availabilities;

    private List<String> groupIds;
}