package com.study.study_platform;

import com.study.study_platform.model.document.*;
import com.study.study_platform.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final ObjectiveRepository objectiveRepository;
    private final StudySessionRepository studySessionRepository;
    private final GroupRepository groupRepository;
    private final MessageRepository messageRepository;
    private final InvitationRepository invitationRepository;
    private final NotificationRepository notificationRepository;

    public DatabaseInitializer(
            UserRepository userRepository,
            SubjectRepository subjectRepository,
            ObjectiveRepository objectiveRepository,
            StudySessionRepository studySessionRepository,
            GroupRepository groupRepository,
            MessageRepository messageRepository,
            InvitationRepository invitationRepository,
            NotificationRepository notificationRepository
    ) {
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.objectiveRepository = objectiveRepository;
        this.studySessionRepository = studySessionRepository;
        this.groupRepository = groupRepository;
        this.messageRepository = messageRepository;
        this.invitationRepository = invitationRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void run(String... args) {

        // 🔥 force création de TOUTES les collections

        userRepository.save(new Utilisateur(null, "test", "test", "test@mail.com", "123", Role.USER, null, null));

        subjectRepository.save(new Subject(null, "Math"));

        objectiveRepository.save(new Objective());

        studySessionRepository.save(new StudySession());

        groupRepository.save(new Group());

        messageRepository.save(new Message());

        invitationRepository.save(new Invitation());

        notificationRepository.save(new Notification());
    }
}