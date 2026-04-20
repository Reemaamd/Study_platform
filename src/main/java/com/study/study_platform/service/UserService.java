package com.study.study_platform.service;


import com.study.study_platform.model.document.Role;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public void createUser() {

        Utilisateur utilisateur = new Utilisateur(
                null,
                "test user",
                "test",
                "test@mail.com",
                "123",
                Role.USER,
                new ArrayList<>(),
                new ArrayList<>()
        );

        userRepository.save(utilisateur);
    }
}