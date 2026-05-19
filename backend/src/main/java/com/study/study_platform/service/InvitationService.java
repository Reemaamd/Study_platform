package com.study.study_platform.service;

import com.study.study_platform.dto.InvitationDTO;
import com.study.study_platform.dto.InvitationResponseDTO;
import com.study.study_platform.mapper.InvitationMapper;
import com.study.study_platform.model.document.Group;
import com.study.study_platform.model.document.Invitation;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.repository.GroupRepository;
import com.study.study_platform.repository.InvitationRepository;
import com.study.study_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final InvitationMapper invitationMapper;

    public InvitationResponseDTO sendInvitation(
            InvitationDTO dto,
            String username
    ) {

        Utilisateur sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        Group group = groupRepository.findById(dto.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!group.getMemberIds().contains(sender.getId())) {
            throw new RuntimeException("You are not a member of this group");
        }

        Utilisateur receiver = userRepository.findById(dto.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        if (group.getMemberIds().contains(receiver.getId())) {
            throw new RuntimeException("User already member of group");
        }

        boolean alreadyExists = invitationRepository
                .findBySenderIdAndReceiverIdAndGroupIdAndStatus(
                        sender.getId(),
                        receiver.getId(),
                        group.getId(),
                        "PENDING"
                ).isPresent();

        if (alreadyExists) {
            throw new RuntimeException("Invitation already exists");
        }

        Invitation invitation = new Invitation();

        invitation.setSenderId(sender.getId());
        invitation.setReceiverId(receiver.getId());
        invitation.setGroupId(group.getId());
        invitation.setStatus("PENDING");
        invitation.setCreatedAt(new Date());

        Invitation saved = invitationRepository.save(invitation);

        return invitationMapper.toResponseDTO(saved);
    }

    public List<Invitation> getMyInvitations(String username) {

        Utilisateur user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return invitationRepository.findByReceiverId(user.getId());
    }

    public String acceptInvitation(String invitationId, String username) {

        Utilisateur user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        if (!invitation.getReceiverId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        if (!invitation.getStatus().equals("PENDING")) {
            throw new RuntimeException("Invitation already processed");
        }

        invitation.setStatus("ACCEPTED");

        invitationRepository.save(invitation);

        Group group = groupRepository.findById(invitation.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        group.getMemberIds().add(user.getId());

        groupRepository.save(group);

        if (user.getGroupIds() == null) {
            user.setGroupIds(new java.util.ArrayList<>());
        }

        user.getGroupIds().add(group.getId());

        userRepository.save(user);

        return "Invitation accepted";
    }

    public String rejectInvitation(String invitationId, String username) {

        Utilisateur user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        if (!invitation.getReceiverId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        invitation.setStatus("REJECTED");

        invitationRepository.save(invitation);

        return "Invitation rejected";
    }
}