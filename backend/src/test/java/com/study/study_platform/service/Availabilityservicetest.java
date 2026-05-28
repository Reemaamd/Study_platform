package com.study.study_platform.service;


import com.study.study_platform.dto.AvailabilityDTO;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.model.embedded.Availability;
import java.time.DayOfWeek;
import com.study.study_platform.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks
    private AvailabilityService availabilityService;

    // ── fixtures ─────────────────────────────────────────────────────────────

    private static final String USERNAME = "bob";

    private Utilisateur userWithAvailabilities(List<Availability> list) {
        Utilisateur u = new Utilisateur();
        u.setId("user-001");
        u.setUsername(USERNAME);
        u.setAvailabilities(new ArrayList<>(list));
        return u;
    }

    private Utilisateur emptyUser() {
        return userWithAvailabilities(new ArrayList<>());
    }

    private Availability avail(DayOfWeek day, String start, String end) {
        Availability a = new Availability();
        a.setDay(day);
        a.setStartTime(start);
        a.setEndTime(end);
        return a;
    }

    private AvailabilityDTO dto(String day, String start, String end) {
        AvailabilityDTO d = new AvailabilityDTO();
        d.setDay(day);
        d.setStartTime(start);
        d.setEndTime(end);
        return d;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getUserAvailability
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserAvailability – returns existing list")
    void getUserAvailability_returnsList() {
        Availability a = avail(DayOfWeek.MONDAY, "09:00", "11:00");
        Utilisateur user = userWithAvailabilities(List.of(a));

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        List<Availability> result = availabilityService.getUserAvailability(USERNAME);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDay()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    @DisplayName("getUserAvailability – null availabilities returns empty list")
    void getUserAvailability_nullReturnsEmpty() {
        Utilisateur user = new Utilisateur();
        user.setUsername(USERNAME);
        user.setAvailabilities(null);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        List<Availability> result = availabilityService.getUserAvailability(USERNAME);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getUserAvailability – user not found throws RuntimeException")
    void getUserAvailability_userNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> availabilityService.getUserAvailability("ghost"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // addAvailability
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("addAvailability – adds new availability and persists")
    void addAvailability_success() {
        Utilisateur user = emptyUser();
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        AvailabilityDTO dto = dto("MONDAY", "09:00", "11:00");
        List<Availability> result = availabilityService.addAvailability(USERNAME, dto);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDay()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(result.get(0).getStartTime()).isEqualTo("09:00");
        assertThat(result.get(0).getEndTime()).isEqualTo("11:00");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("addAvailability – initialises null list before adding")
    void addAvailability_nullListInitialised() {
        Utilisateur user = new Utilisateur();
        user.setId("u1");
        user.setUsername(USERNAME);
        user.setAvailabilities(null);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        AvailabilityDTO dto = dto("TUESDAY", "10:00", "12:00");
        List<Availability> result = availabilityService.addAvailability(USERNAME, dto);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("addAvailability – duplicate throws RuntimeException")
    void addAvailability_duplicate() {
        Availability existing = avail(DayOfWeek.MONDAY, "09:00", "11:00");
        Utilisateur user = userWithAvailabilities(List.of(existing));

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        AvailabilityDTO dto = dto("MONDAY", "09:00", "11:00");

        assertThatThrownBy(() -> availabilityService.addAvailability(USERNAME, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Availability already exists");
    }

    @Test
    @DisplayName("addAvailability – overlapping time throws RuntimeException")
    void addAvailability_overlap() {
        Availability existing = avail(DayOfWeek.MONDAY, "09:00", "12:00");
        Utilisateur user = userWithAvailabilities(List.of(existing));

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        // overlaps with 09:00-12:00
        AvailabilityDTO dto = dto("MONDAY", "10:00", "13:00");

        assertThatThrownBy(() -> availabilityService.addAvailability(USERNAME, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Time overlap detected");
    }

    @Test
    @DisplayName("addAvailability – different day does not trigger overlap check")
    void addAvailability_differentDayNoOverlap() {
        Availability existing = avail(DayOfWeek.MONDAY, "09:00", "12:00");
        Utilisateur user = userWithAvailabilities(List.of(existing));

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        // Same hours but Tuesday → no conflict
        AvailabilityDTO dto = dto("TUESDAY", "09:00", "12:00");
        List<Availability> result = availabilityService.addAvailability(USERNAME, dto);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("addAvailability – end before start throws RuntimeException")
    void addAvailability_invalidTimeRange() {
        Utilisateur user = emptyUser();
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        AvailabilityDTO dto = dto("MONDAY", "12:00", "10:00");

        assertThatThrownBy(() -> availabilityService.addAvailability(USERNAME, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid time range");
    }

    @Test
    @DisplayName("addAvailability – equal start/end throws RuntimeException")
    void addAvailability_sameStartEnd() {
        Utilisateur user = emptyUser();
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        AvailabilityDTO dto = dto("MONDAY", "10:00", "10:00");

        assertThatThrownBy(() -> availabilityService.addAvailability(USERNAME, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid time range");
    }

    @Test
    @DisplayName("addAvailability – adjacent slots (no gap) are NOT an overlap")
    void addAvailability_adjacentSlots_noOverlap() {
        Availability existing = avail(DayOfWeek.MONDAY, "09:00", "11:00");
        Utilisateur user = userWithAvailabilities(List.of(existing));

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        // 11:00-13:00 touches but doesn't overlap 09:00-11:00
        AvailabilityDTO dto = dto("MONDAY", "11:00", "13:00");
        List<Availability> result = availabilityService.addAvailability(USERNAME, dto);

        assertThat(result).hasSize(2);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateAvailability
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateAvailability – updates slot at given index")
    void updateAvailability_success() {
        Availability old = avail(DayOfWeek.MONDAY, "09:00", "11:00");
        Utilisateur user = userWithAvailabilities(List.of(old));

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        AvailabilityDTO dto = dto("MONDAY", "10:00", "12:00");
        List<Availability> result = availabilityService.updateAvailability(USERNAME, 0, dto);

        assertThat(result.get(0).getStartTime()).isEqualTo("10:00");
        assertThat(result.get(0).getEndTime()).isEqualTo("12:00");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("updateAvailability – negative index throws RuntimeException")
    void updateAvailability_negativeIndex() {
        Utilisateur user = emptyUser();
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        AvailabilityDTO dto = dto("MONDAY", "09:00", "11:00");

        assertThatThrownBy(() -> availabilityService.updateAvailability(USERNAME, -1, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid index");
    }

    @Test
    @DisplayName("updateAvailability – out-of-bound index throws RuntimeException")
    void updateAvailability_outOfBounds() {
        Availability a = avail(DayOfWeek.MONDAY, "09:00", "11:00");
        Utilisateur user = userWithAvailabilities(List.of(a));

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        AvailabilityDTO dto = dto("MONDAY", "09:00", "11:00");

        assertThatThrownBy(() -> availabilityService.updateAvailability(USERNAME, 5, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid index");
    }

    @Test
    @DisplayName("updateAvailability – overlap with another slot throws RuntimeException")
    void updateAvailability_overlapWithOtherSlot() {
        Availability first  = avail(DayOfWeek.MONDAY, "09:00", "11:00");
        Availability second = avail(DayOfWeek.MONDAY, "13:00", "15:00");
        Utilisateur user = userWithAvailabilities(List.of(first, second));

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        // Update index 0 to overlap with second (13:00-15:00)
        AvailabilityDTO dto = dto("MONDAY", "12:00", "14:00");

        assertThatThrownBy(() -> availabilityService.updateAvailability(USERNAME, 0, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Time overlap detected");
    }

    @Test
    @DisplayName("updateAvailability – updating slot does not consider itself as overlap")
    void updateAvailability_selfOverlapAllowed() {
        Availability a = avail(DayOfWeek.MONDAY, "09:00", "11:00");
        Utilisateur user = userWithAvailabilities(List.of(a));

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        // Extending the same slot — should not trigger self-overlap
        AvailabilityDTO dto = dto("MONDAY", "09:00", "13:00");
        List<Availability> result = availabilityService.updateAvailability(USERNAME, 0, dto);

        assertThat(result.get(0).getEndTime()).isEqualTo("13:00");
    }

    @Test
    @DisplayName("updateAvailability – invalid time range throws RuntimeException")
    void updateAvailability_invalidRange() {
        Availability a = avail(DayOfWeek.MONDAY, "09:00", "11:00");
        Utilisateur user = userWithAvailabilities(List.of(a));

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        AvailabilityDTO dto = dto("MONDAY", "11:00", "09:00");

        assertThatThrownBy(() -> availabilityService.updateAvailability(USERNAME, 0, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid time range");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteAvailability
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteAvailability – removes element at given index")
    void deleteAvailability_success() {
        Availability a = avail(DayOfWeek.MONDAY, "09:00", "11:00");
        Utilisateur user = userWithAvailabilities(List.of(a));

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        availabilityService.deleteAvailability(USERNAME, 0);

        assertThat(user.getAvailabilities()).isEmpty();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("deleteAvailability – out-of-bound index does nothing")
    void deleteAvailability_outOfBounds_noEffect() {
        Availability a = avail(DayOfWeek.MONDAY, "09:00", "11:00");
        Utilisateur user = userWithAvailabilities(List.of(a));

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        availabilityService.deleteAvailability(USERNAME, 99);

        // List unchanged, save never called
        assertThat(user.getAvailabilities()).hasSize(1);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteAvailability – negative index does nothing")
    void deleteAvailability_negativeIndex_noEffect() {
        Availability a = avail(DayOfWeek.MONDAY, "09:00", "11:00");
        Utilisateur user = userWithAvailabilities(List.of(a));

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        availabilityService.deleteAvailability(USERNAME, -1);

        assertThat(user.getAvailabilities()).hasSize(1);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteAvailability – null availabilities list does nothing")
    void deleteAvailability_nullList_noEffect() {
        Utilisateur user = new Utilisateur();
        user.setUsername(USERNAME);
        user.setAvailabilities(null);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        // Should not throw
        assertThatCode(() -> availabilityService.deleteAvailability(USERNAME, 0))
                .doesNotThrowAnyException();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteAvailability – correct element removed when multiple slots exist")
    void deleteAvailability_correctElementRemoved() {
        Availability first  = avail(DayOfWeek.MONDAY,    "09:00", "11:00");
        Availability second = avail(DayOfWeek.WEDNESDAY, "14:00", "16:00");
        Utilisateur user = userWithAvailabilities(List.of(first, second));

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        availabilityService.deleteAvailability(USERNAME, 0);

        assertThat(user.getAvailabilities()).hasSize(1);
        assertThat(user.getAvailabilities().get(0).getDay())
                .isEqualTo(DayOfWeek.WEDNESDAY);
    }
}
