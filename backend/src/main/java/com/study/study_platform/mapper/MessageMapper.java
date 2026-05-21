package com.study.study_platform.mapper;

import com.study.study_platform.dto.MessageResponseDTO;
import com.study.study_platform.model.document.Message;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public MessageResponseDTO toDTO(Message message) {

        MessageResponseDTO dto = new MessageResponseDTO();

        dto.setId(message.getId());
        dto.setGroupId(message.getGroupId());
        dto.setSenderId(message.getSenderId());
        dto.setContent(message.getContent());
        dto.setCreatedAt(message.getCreatedAt());

        return dto;
    }
}