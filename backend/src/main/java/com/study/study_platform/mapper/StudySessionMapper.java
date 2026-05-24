package com.study.study_platform.mapper;

import com.study.study_platform.dto.StudySessionDTO;
import com.study.study_platform.model.document.StudySession;
import com.study.study_platform.model.enums.SessionStatus;
import org.springframework.stereotype.Component;

@Component
public class StudySessionMapper {

    public StudySession toEntity(StudySessionDTO dto, String userId) {

        return StudySession.builder()
                .userId(userId)
                .subjectId(dto.getSubjectId())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .status(dto.getStatus())
                .build();
    }

  /*  public StudySessionDTO toDTO(StudySession session) {

        StudySessionDTO dto = new StudySessionDTO();
        dto.setSubjectId(session.getSubjectId());
        dto.setStartTime(session.getStartTime());
        dto.setEndTime(session.getEndTime());
        dto.setStatus(session.getStatus().name());

        return dto;
    }*/
}