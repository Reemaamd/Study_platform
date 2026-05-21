package com.study.study_platform.service;

import com.study.study_platform.dto.MessageResponseDTO;
import com.study.study_platform.mapper.MessageMapper;
import com.study.study_platform.model.document.Group;
import com.study.study_platform.model.document.Message;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.repository.GroupRepository;
import com.study.study_platform.repository.MessageRepository;
import com.study.study_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageMapper messageMapper;
    private final MessageRepository messageRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public MessageResponseDTO sendMessage(
            String groupId,
            String username,
            String content
    ) {

        Utilisateur sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String senderId = sender.getId();

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!group.getMemberIds().contains(senderId)) {
            throw new RuntimeException("Not a group member");
        }

        Message message = new Message();

        message.setGroupId(groupId);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());

        Message saved = messageRepository.save(message);

        return messageMapper.toDTO(saved);
    }

    public List<Message> getGroupMessages(String groupId) {
        return messageRepository.findByGroupIdOrderByCreatedAtAsc(groupId);
    }
}
