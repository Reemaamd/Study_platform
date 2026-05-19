package com.study.study_platform.mapper;

import com.study.study_platform.dto.GroupDTO;
import com.study.study_platform.dto.GroupResponseDTO;
import com.study.study_platform.model.document.Group;
import org.springframework.stereotype.Component;

@Component
public class GroupMapper {

    public Group toEntity(GroupDTO dto) {

        Group group = new Group();

        group.setName(dto.getName());

        return group;
    }

    public GroupResponseDTO toResponseDTO(Group group) {

        return GroupResponseDTO.builder()
                .id(group.getId())
                .name(group.getName())
                .ownerId(group.getOwnerId())
                .memberIds(group.getMemberIds())
                .createdAt(group.getCreatedAt())
                .build();
    }
}