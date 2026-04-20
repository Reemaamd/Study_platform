package com.study.study_platform.repository;

import com.study.study_platform.model.document.Invitation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InvitationRepository extends MongoRepository<Invitation, String> {
}