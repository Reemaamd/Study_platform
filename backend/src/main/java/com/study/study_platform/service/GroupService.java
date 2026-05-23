package com.study.study_platform.service;

import com.study.study_platform.dto.GroupDTO;
import com.study.study_platform.dto.GroupResponseDTO;
import com.study.study_platform.mapper.GroupMapper;
import com.study.study_platform.model.document.Group;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.repository.GroupRepository;
import com.study.study_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupMapper groupMapper;

    public GroupResponseDTO createGroup(GroupDTO dto, String username) {

        if (groupRepository.existsByName(dto.getName())) {
            throw new RuntimeException("Group name already exists");
        }

        Utilisateur user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String userId = user.getId();

        Group group = groupMapper.toEntity(dto);

        group.setOwnerId(userId);

        group.setCreatedAt(LocalDateTime.now());

        group.setMemberIds(List.of(userId));

        Group savedGroup = groupRepository.save(group);
        if (user.getGroupIds() == null) {
            user.setGroupIds(new ArrayList<>());
        }

        user.getGroupIds().add(savedGroup.getId());

        userRepository.save(user);

        return groupMapper.toResponseDTO(savedGroup);
    }

    public List<Group> getMyGroups(String username) {

        Utilisateur user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return groupRepository.findByMemberIdsContaining(user.getId());
    }

    public Group getGroupDetails(String groupId) {

        return groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
    }

    public void leaveGroup(String groupId, String username) {

        Utilisateur user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String userId = user.getId();

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (group.getOwnerId().equals(userId)) {
            throw new RuntimeException("Owner cannot leave the group");
        }

        group.getMemberIds().remove(userId);

        groupRepository.save(group);

        user.getGroupIds().remove(groupId);

        userRepository.save(user);
    }

    public void deleteGroup(String groupId, String username) {

        Utilisateur user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String userId = user.getId();

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!group.getOwnerId().equals(userId)) {
            throw new RuntimeException("Only owner can delete group");
        }

        for (String memberId : group.getMemberIds()) {

            Utilisateur member = userRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            member.getGroupIds().remove(groupId);

            userRepository.save(member);
        }

        groupRepository.delete(group);
    }
    public GroupResponseDTO updateGroup(
            String groupId,
            GroupDTO dto,
            String username
    ) {

        Utilisateur user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!group.getOwnerId().equals(user.getId())) {
            throw new RuntimeException("Only owner can update group");
        }

        if (groupRepository.existsByName(dto.getName())
                && !group.getName().equals(dto.getName())) {

            throw new RuntimeException("Group name already exists");
        }

        group.setName(dto.getName());

        Group updated = groupRepository.save(group);

        return groupMapper.toResponseDTO(updated);
    }
    public void removeMember(
            String groupId,
            String memberId,
            String username
    ) {

        Utilisateur owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!group.getOwnerId().equals(owner.getId())) {
            throw new RuntimeException("Only owner can remove members");
        }

        if (group.getOwnerId().equals(memberId)) {
            throw new RuntimeException("Owner cannot remove himself");
        }

        if (!group.getMemberIds().contains(memberId)) {
            throw new RuntimeException("User is not member of group");
        }

        group.getMemberIds().remove(memberId);

        groupRepository.save(group);

        Utilisateur member = userRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        if (member.getGroupIds() != null) {
            member.getGroupIds().remove(groupId);
        }

        userRepository.save(member);
    }
}