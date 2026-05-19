package com.study.study_platform.service;

import com.study.study_platform.dto.AvailabilityDTO;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.model.embedded.Availability;
import com.study.study_platform.model.enums.DayOfWeek;
import com.study.study_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final UserRepository userRepository;

    // ==================== GET ====================
    public List<Availability> getUserAvailability(String username) {

        Utilisateur user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user.getAvailabilities() != null
                ? user.getAvailabilities()
                : new ArrayList<>();
    }

    // ==================== CREATE ====================
    public List<Availability> addAvailability(String username, AvailabilityDTO dto) {

        Utilisateur user = getUser(username);

        if (user.getAvailabilities() == null) {
            user.setAvailabilities(new ArrayList<>());
        }

        validate(dto);

        DayOfWeek day = DayOfWeek.valueOf(dto.getDay());
        LocalTime newStart = LocalTime.parse(dto.getStartTime());
        LocalTime newEnd = LocalTime.parse(dto.getEndTime());

        // ❌ Check doublon
        boolean exists = user.getAvailabilities().stream().anyMatch(a ->
                a.getDay().equals(day) &&
                        a.getStartTime().equals(dto.getStartTime()) &&
                        a.getEndTime().equals(dto.getEndTime())
        );

        if (exists) {
            throw new RuntimeException("Availability already exists");
        }

        // ❌ Check overlap
        boolean overlap = user.getAvailabilities().stream().anyMatch(a ->
                a.getDay().equals(day) &&
                        newStart.isBefore(LocalTime.parse(a.getEndTime())) &&
                        newEnd.isAfter(LocalTime.parse(a.getStartTime()))
        );

        if (overlap) {
            throw new RuntimeException("Time overlap detected");
        }

        // ✅ Create
        Availability availability = new Availability();
        availability.setDay(day);
        availability.setStartTime(dto.getStartTime());
        availability.setEndTime(dto.getEndTime());

        user.getAvailabilities().add(availability);

        userRepository.save(user);

        return user.getAvailabilities();
    }

    // ==================== UPDATE ====================
    public List<Availability> updateAvailability(
            String username,
            int index,
            AvailabilityDTO dto) {

        Utilisateur user = getUser(username);

        if (user.getAvailabilities() == null ||
                index < 0 ||
                index >= user.getAvailabilities().size()) {
            throw new RuntimeException("Invalid index");
        }

        validate(dto);

        DayOfWeek day = DayOfWeek.valueOf(dto.getDay());
        LocalTime newStart = LocalTime.parse(dto.getStartTime());
        LocalTime newEnd = LocalTime.parse(dto.getEndTime());

        // ❌ Check overlap (ignorer l’élément actuel)
        boolean overlap = user.getAvailabilities().stream()
                .filter(a -> user.getAvailabilities().indexOf(a) != index)
                .anyMatch(a ->
                        a.getDay().equals(day) &&
                                newStart.isBefore(LocalTime.parse(a.getEndTime())) &&
                                newEnd.isAfter(LocalTime.parse(a.getStartTime()))
                );

        if (overlap) {
            throw new RuntimeException("Time overlap detected");
        }

        Availability updated = new Availability();
        updated.setDay(day);
        updated.setStartTime(dto.getStartTime());
        updated.setEndTime(dto.getEndTime());

        user.getAvailabilities().set(index, updated);

        userRepository.save(user);

        return user.getAvailabilities();
    }

    // ==================== DELETE ====================
    public void deleteAvailability(String username, int index) {

        Utilisateur user = getUser(username);

        if (user.getAvailabilities() != null &&
                index >= 0 &&
                index < user.getAvailabilities().size()) {

            user.getAvailabilities().remove(index);
            userRepository.save(user);
        }
    }

    // ==================== VALIDATION ====================
    private void validate(AvailabilityDTO dto) {

        LocalTime start = LocalTime.parse(dto.getStartTime());
        LocalTime end = LocalTime.parse(dto.getEndTime());

        if (!end.isAfter(start)) {
            throw new RuntimeException("Invalid time range");
        }
    }

    // ==================== UTIL ====================
    private Utilisateur getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}