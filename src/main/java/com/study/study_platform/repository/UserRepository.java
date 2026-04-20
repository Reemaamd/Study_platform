package com.study.study_platform.repository;


import com.study.study_platform.model.document.Utilisateur;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<Utilisateur, String> {
    Optional<Utilisateur> findByUsername(String username);
}
