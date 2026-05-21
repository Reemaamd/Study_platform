package com.study.study_platform.service;

import com.study.study_platform.dto.CommonAvailabilityDTO;
import com.study.study_platform.model.document.Group;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.model.embedded.Availability;
import com.study.study_platform.repository.GroupRepository;
import com.study.study_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CollaborativeSessionService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public List<CommonAvailabilityDTO> findCommonAvailabilities(
            String groupId
    ) {

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() ->
                        new RuntimeException("Group not found"));

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

                result.add(
                        new CommonAvailabilityDTO(
                                day,
                                commonStart.toString(),
                                commonEnd.toString()
                        )
                );
            }
        }

        return result;
    }
}