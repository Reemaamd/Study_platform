package com.study.study_platform.mapper;

import com.study.study_platform.dto.CollaborativeSessionResponseDTO;
import com.study.study_platform.model.document.StudySession;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CollaborativeSessionMapper {

    private final UserRepository userRepository;

    public CollaborativeSessionResponseDTO toDTO(
            StudySession session
    ) {

        Utilisateur user =
                userRepository
                        .findById(session.getUserId())
                        .orElse(null);

        return CollaborativeSessionResponseDTO.builder()

                .id(session.getId())

                .subjectId(session.getSubjectId())

                .groupId(session.getGroupId())

                .userUsername(
                        user != null
                                ? user.getUsername()
                                : "Unknown"
                )

                .type(
                        session.getType().name()
                )

                .status(
                        session.getStatus().name()
                )

                .startTime(
                        session.getStartTime()
                )

                .endTime(
                        session.getEndTime()
                )

                .build();
    }
}