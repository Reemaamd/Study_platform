package com.study.study_platform.dto;
import lombok.*;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubjectStatsDTO {

    // Champ existant (gardé pour compatibilité)
    private String subjectName;
    private long totalHours;
    private double progressPercentage;

    // Aliases pour le frontend Angular
    public String getSubject() {
        return subjectName;                 // template utilise subj.subject
    }

    public double getPercentage() {
        return progressPercentage;          // template utilise subj.percentage
    }

    public String getColor() {
        // Couleur auto selon le nom (optionnel)
        if (subjectName == null) return "#4CAF7D";
        return switch (subjectName.toLowerCase()) {
            case "mathématiques", "algèbre", "maths" -> "#4CAF7D";
            case "algorithmique", "informatique"      -> "#F59E0B";
            case "histoire"                           -> "#3B82F6";
            case "physique"                           -> "#8B5CF6";
            default -> "#4CAF7D";
        };
    }
}
