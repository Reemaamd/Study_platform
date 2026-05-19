package com.study.study_platform.repository;

import com.study.study_platform.model.document.Invitation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends MongoRepository<Invitation, String> {

    List<Invitation> findByReceiverId(String receiverId);

    List<Invitation> findBySenderId(String senderId);

    Optional<Invitation>
    findBySenderIdAndReceiverIdAndGroupIdAndStatus(
            String senderId,
            String receiverId,
            String groupId,
            String status
    );
}