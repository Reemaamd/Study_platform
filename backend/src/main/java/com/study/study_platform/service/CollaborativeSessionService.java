package com.study.study_platform.service;

import com.study.study_platform.dto.CommonAvailabilityDTO;
import com.study.study_platform.model.document.Group;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.model.embedded.Availability;
import com.study.study_platform.repository.GroupRepository;
import com.study.study_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.study.study_platform.model.document.Subject;
import com.study.study_platform.repository.SubjectRepository;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import com.study.study_platform.model.document.StudySession;
import com.study.study_platform.repository.StudySessionRepository;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.LocalDateTime;
import com.study.study_platform.dto.CollaborativeSessionDTO;
import com.study.study_platform.model.document.StudySession;
import com.study.study_platform.model.enums.SessionStatus;
import com.study.study_platform.model.enums.SessionType;

@Service
@RequiredArgsConstructor
public class CollaborativeSessionService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final StudySessionRepository studySessionRepository;
    private final SubjectRepository subjectRepository;

    public List<CommonAvailabilityDTO> findCommonAvailabilities(
            String groupId,
            String username
    ){
        Utilisateur currentUser =
                userRepository.findByUsername(username)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Group not found"
                        )
                );

        if (!group.getMemberIds().contains(
                currentUser.getId()
        )) {

            throw new RuntimeException(
                    "You are not member of this group"
            );
        }
        updateExpiredSessions();

        List<Utilisateur> members = userRepository
                .findAllById(group.getMemberIds());

        if (members.isEmpty()) {
            throw new RuntimeException("No members found");
        }

        List<CommonAvailabilityDTO> result = new ArrayList<>();

        Utilisateur firstMember = members.get(0);

        for (Availability baseAvailability :
                firstMember.getAvailabilities()) {

            String day = baseAvailability.getDay().name();

            LocalTime commonStart =
                    LocalTime.parse(baseAvailability.getStartTime());

            LocalTime commonEnd =
                    LocalTime.parse(baseAvailability.getEndTime());

            boolean valid = true;

            for (int i = 1; i < members.size(); i++) {

                Utilisateur member = members.get(i);

                Availability matchingAvailability =
                        member.getAvailabilities()
                                .stream()
                                .filter(av ->
                                        av.getDay().name().equals(day))
                                .findFirst()
                                .orElse(null);

                if (matchingAvailability == null) {
                    valid = false;
                    break;
                }

                LocalTime memberStart =
                        LocalTime.parse(
                                matchingAvailability.getStartTime());

                LocalTime memberEnd =
                        LocalTime.parse(
                                matchingAvailability.getEndTime());

                if (memberStart.isAfter(commonStart)) {
                    commonStart = memberStart;
                }

                if (memberEnd.isBefore(commonEnd)) {
                    commonEnd = memberEnd;
                }

                if (!commonStart.isBefore(commonEnd)) {
                    valid = false;
                    break;
                }
            }

            if (valid) {

                LocalDate today = LocalDate.now();

                LocalDate weekStart =
                        today.with(java.time.DayOfWeek.MONDAY);

                LocalDate weekEnd =
                        weekStart.plusDays(6);

                LocalDate slotDate =
                        weekStart.with(
                                TemporalAdjusters.nextOrSame(
                                        java.time.DayOfWeek.valueOf(day)
                                )
                        );

                // ==========================
                // SLOT MUST BE THIS WEEK
                // ==========================
                if (slotDate.isAfter(weekEnd)) {
                    continue;
                }

                LocalDateTime slotStart =
                        slotDate.atTime(commonStart);

                LocalDateTime slotEnd =
                        slotDate.atTime(commonEnd);

                // ==========================
                // SLOT MUST NOT BE PASSED
                // ==========================
                if (slotStart.isBefore(LocalDateTime.now())) {
                    continue;
                }

                boolean occupied = false;

                for (Utilisateur member : members) {

                    if (hasConflict(
                            member.getId(),
                            slotStart,
                            slotEnd
                    )) {

                        occupied = true;
                        break;
                    }
                }

                if (!occupied) {

                    result.add(
                            new CommonAvailabilityDTO(
                                    day,
                                    commonStart.toString(),
                                    commonEnd.toString()
                            )
                    );
                }
            }
        }

        return result;
    }
    private boolean hasConflict(
            String userId,
            LocalDateTime start,
            LocalDateTime end
    ) {

        List<StudySession> sessions =
                studySessionRepository.findByUserId(userId);

        return sessions.stream().anyMatch(session ->

                start.isBefore(session.getEndTime())
                        &&
                        end.isAfter(session.getStartTime())
        );
    }
    public StudySession createCollaborativeSession(
            CollaborativeSessionDTO dto,
            String username
    ) {
        updateExpiredSessions();

        Utilisateur creator = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Group group = groupRepository.findById(dto.getGroupId())
                .orElseThrow(() ->
                        new RuntimeException("Group not found"));
        Subject subject = subjectRepository
                .findById(dto.getSubjectId())
                .orElseThrow(() ->
                        new RuntimeException("Subject not found"));

        if (!subject.getUserId().equals(creator.getId())) {

            throw new RuntimeException(
                    "You can only use your own subjects"
            );
        }
        for (String memberId : group.getMemberIds()) {

            boolean exists =
                    subjectRepository.findAll()
                            .stream()
                            .anyMatch(sub ->

                                    sub.getUserId().equals(memberId)
                                            &&
                                            sub.getName().equalsIgnoreCase(
                                                    subject.getName()
                                            )
                            );

            if (!exists) {

                Subject newSubject = new Subject();

                newSubject.setName(subject.getName());

                newSubject.setUserId(memberId);

                subjectRepository.save(newSubject);
            }
        }
        if (!dto.getStartTime().isBefore(dto.getEndTime())) {

            throw new RuntimeException(
                    "Start time must be before end time"
            );
        }

        if (dto.getStartTime().isBefore(
                LocalDateTime.now()
        )) {

            throw new RuntimeException(
                    "Cannot create session in the past"
            );
        }
        String sessionDay =
                dto.getStartTime()
                        .getDayOfWeek()
                        .name();

        boolean validSlot = false;

        List<CommonAvailabilityDTO> commonSlots =
                findCommonAvailabilities(
                        dto.getGroupId(),
                        username
                );

        for (CommonAvailabilityDTO slot : commonSlots) {

            LocalTime slotStart =
                    LocalTime.parse(slot.getStartTime());

            LocalTime slotEnd =
                    LocalTime.parse(slot.getEndTime());

            if (
                    slot.getDay().equals(sessionDay)
                            &&
                            slotStart.equals(
                                    dto.getStartTime().toLocalTime()
                            )
                            &&
                            slotEnd.equals(
                                    dto.getEndTime().toLocalTime()
                            )
            ) {

                validSlot = true;
                break;
            }
        }

        if (!validSlot) {

            throw new RuntimeException(
                    "Session must match a valid common availability"
            );
        }
        // ==============================
        // CHECK USER IS GROUP MEMBER
        // ==============================
        if (!group.getMemberIds().contains(creator.getId())) {

            throw new RuntimeException(
                    "You are not member of this group"
            );
        }

        // ==============================
        // CHECK ALL MEMBERS CONFLICTS
        // ==============================
        for (String memberId : group.getMemberIds()) {

            boolean conflict = hasConflict(
                    memberId,
                    dto.getStartTime(),
                    dto.getEndTime()
            );

            if (conflict) {

                throw new RuntimeException(
                        "One or more members already have session conflict"
                );
            }
        }

        // ==============================
        // CREATE SESSION
        // ==============================
        StudySession session = StudySession.builder()
                .userId(creator.getId())
                .groupId(group.getId())
                .subjectId(dto.getSubjectId())
                .participantIds(new ArrayList<>(group.getMemberIds()))
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .type(SessionType.COLLABORATIVE)
                .status(SessionStatus.PLANNED)
                .createdAt(LocalDateTime.now())
                .build();

        return studySessionRepository.save(session);
    }
    private void updateExpiredSessions() {

        List<StudySession> sessions =
                studySessionRepository.findAll();

        LocalDateTime now = LocalDateTime.now();

        for (StudySession session : sessions) {

            if (
                    session.getType() == SessionType.COLLABORATIVE
                            &&
                            now.isAfter(session.getEndTime())
            ) {

                if (session.getStatus() ==
                        SessionStatus.PLANNED) {

                    session.setStatus(
                            SessionStatus.CANCELLED
                    );

                    studySessionRepository.save(session);
                }

                else if (session.getStatus() ==
                        SessionStatus.ONGOING) {

                    session.setStatus(
                            SessionStatus.DONE
                    );

                    studySessionRepository.save(session);
                }
            }
        }
    }
    public StudySession startSession(
            String sessionId,
            String username
    ) {

        StudySession session =
                studySessionRepository.findById(sessionId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Session not found"));

        Utilisateur user =
                userRepository.findByUsername(username)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"));

        if (!session.getUserId().equals(user.getId())) {

            throw new RuntimeException(
                    "Only owner can start session"
            );
        }

        if (session.getStatus() != SessionStatus.PLANNED) {

            throw new RuntimeException(
                    "Session cannot be started"
            );
        }
        if (LocalDateTime.now().isAfter(
                session.getEndTime()
        )) {

            throw new RuntimeException(
                    "Session already expired"
            );
        }
        if (LocalDateTime.now().isBefore(
                session.getStartTime()
        )) {

            throw new RuntimeException(
                    "Session cannot start before start time"
            );
        }
        session.setStatus(SessionStatus.ONGOING);

        return studySessionRepository.save(session);
    }
    public StudySession completeSession(
            String sessionId,
            String username
    ) {

        StudySession session =
                studySessionRepository.findById(sessionId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Session not found"));

        Utilisateur user =
                userRepository.findByUsername(username)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"));

        if (!session.getUserId().equals(user.getId())) {

            throw new RuntimeException(
                    "Only owner can complete session"
            );
        }

        if (session.getStatus() != SessionStatus.ONGOING) {

            throw new RuntimeException(
                    "Session must be ongoing"
            );
        }

        session.setStatus(SessionStatus.DONE);

        return studySessionRepository.save(session);
    }
    public StudySession cancelSession(
            String sessionId,
            String username
    ) {

        StudySession session =
                studySessionRepository.findById(sessionId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Session not found"));

        Utilisateur user =
                userRepository.findByUsername(username)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"));

        if (!session.getUserId().equals(user.getId())) {

            throw new RuntimeException(
                    "Only owner can cancel session"
            );
        }

        if (session.getStatus() == SessionStatus.DONE) {

            throw new RuntimeException(
                    "Completed session cannot be cancelled"
            );
        }

        session.setStatus(SessionStatus.CANCELLED);

        return studySessionRepository.save(session);
    }
    public void deleteSession(
            String sessionId,
            String username
    ) {

        StudySession session =
                studySessionRepository.findById(sessionId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Session not found"));

        Utilisateur user =
                userRepository.findByUsername(username)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"));

        if (!session.getUserId().equals(user.getId())) {

            throw new RuntimeException(
                    "Only owner can delete session"
            );
        }
        if (session.getStatus() ==
                SessionStatus.ONGOING) {

            throw new RuntimeException(
                    "Ongoing session cannot be deleted"
            );
        }

        studySessionRepository.delete(session);
    }
}