package com.study.study_platform.mapper;

import com.study.study_platform.dto.ObjectiveDTO;
import com.study.study_platform.dto.ObjectiveRequest;
import com.study.study_platform.model.document.Objective;
import com.study.study_platform.model.document.Subject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ObjectiveMapper {



    public Objective toEntity(ObjectiveRequest request, String userId) {
        Objective obj = new Objective();

        obj.setUserId(userId);
        obj.setSubjectId(request.getSubjectId());
        obj.setWeeklyGoal(request.getWeeklyGoal());
        obj.setPriority(request.getPriority());
        obj.setTitle(request.getTitle()); // ✅ AJOUT
        obj.setWeekStartDate(request.getWeekStartDate());
        obj.setWeekEndDate(request.getWeekEndDate());
        obj.setProgress(0);

        return obj;
    }

    public ObjectiveDTO toDTO(Objective obj) {
        ObjectiveDTO dto = new ObjectiveDTO();

        dto.setId(obj.getId());
        dto.setSubjectId(obj.getSubjectId());
        dto.setWeeklyGoal(obj.getWeeklyGoal());
        dto.setProgress(obj.getProgress());
        dto.setPriority(obj.getPriority());
        dto.setTitle(obj.getTitle()); // ✅ AJOUT
        dto.setWeekStartDate(obj.getWeekStartDate());
        dto.setWeekEndDate(obj.getWeekEndDate());
        //subjectRepository.findById(obj.getSubjectId())
               // .ifPresent(subject -> dto.setSubjectName(subject.getName()));

        // ✅ progressPercentage
        int percentage = 0;
        if (obj.getWeeklyGoal() != 0) {
            percentage = (obj.getProgress() * 100) / obj.getWeeklyGoal();
        }
        dto.setProgressPercentage(percentage);

        // ✅ status avec LATE
        dto.setStatus(computeStatus(obj));


        return dto;
    }

    private String computeStatus(Objective obj) {
        boolean achieved = obj.getProgress() >= obj.getWeeklyGoal();

        boolean late = obj.getWeekEndDate() != null
                && obj.getWeekEndDate().isBefore(LocalDate.now())
                && !achieved;

        if (achieved) return "ACHIEVED";
        if (late)     return "LATE";
        return "IN_PROGRESS";
    }
}