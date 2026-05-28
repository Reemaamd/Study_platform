package com.study.study_platform.mapper;

import com.study.study_platform.dto.GroupDTO;
import com.study.study_platform.dto.GroupResponseDTO;
import com.study.study_platform.dto.MemberDTO;

import com.study.study_platform.model.document.Group;
import com.study.study_platform.model.document.Utilisateur;

import com.study.study_platform.repository.UserRepository;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GroupMapper {

    private final UserRepository userRepository;

    public GroupMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Group toEntity(GroupDTO dto) {

        Group group = new Group();

        group.setName(dto.getName());

        return group;
    }

    public GroupResponseDTO toResponseDTO(Group group) {

        Utilisateur owner = userRepository
                .findById(group.getOwnerId())
                .orElse(null);

        List<MemberDTO> members = (

                group.getMemberIds() != null
                        ? group.getMemberIds()
                        : List.<String>of()

        ).stream()
                .map(memberId -> {

                    Utilisateur user = userRepository
                            .findById(memberId)
                            .orElse(null);

                    return MemberDTO.builder()
                            .id(memberId)
                            .username(
                                    user != null
                                            ? user.getUsername()
                                            : "Unknown"
                            )
                            .owner(
                                    group.getOwnerId()
                                            .equals(memberId)
                            )
                            .build();

                })
                .toList();

        return GroupResponseDTO.builder()

                .id(group.getId())

                .name(group.getName())

                .ownerId(group.getOwnerId())

                .ownerUsername(
                        owner != null
                                ? owner.getUsername()
                                : "Unknown"
                )

                .members(members)

                .createdAt(group.getCreatedAt())

                .build();
    }
}