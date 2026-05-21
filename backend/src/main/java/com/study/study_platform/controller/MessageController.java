package com.study.study_platform.controller;
import com.study.study_platform.dto.GroupDTO;
import com.study.study_platform.dto.GroupResponseDTO;
import com.study.study_platform.dto.MessageRequestDTO;
import com.study.study_platform.dto.MessageResponseDTO;
import com.study.study_platform.model.document.Group;
import com.study.study_platform.model.document.Message;
import com.study.study_platform.service.GroupService;
import com.study.study_platform.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/{groupId}")
    public MessageResponseDTO sendMessage(
            @PathVariable String groupId,
            @AuthenticationPrincipal UserDetails user,
            @RequestBody MessageRequestDTO dto
    ) {
        return messageService.sendMessage(
                groupId,
                user.getUsername(),
                dto.getContent()
        );
    }

    @GetMapping("/{groupId}")
    public List<Message> getMessages(@PathVariable String groupId) {
        return messageService.getGroupMessages(groupId);
    }
}
