package com.study.study_platform.service;

import com.study.study_platform.dto.StudySessionDTO;
import com.study.study_platform.model.document.*;

import com.study.study_platform.model.embedded.Availability;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import com.study.study_platform.model.enums.SessionStatus;
import com.study.study_platform.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudySessionService — Tests Unitaires")
class StudySessionServiceTest {

    @Mock StudySessionRepository repository;
    @Mock UserRepository userRepository;
    @Mock ObjectiveRepository objectiveRepository;
    @Mock
    NotificationService notificationService;
    @Mock SubjectRepository subjectRepository;

    @InjectMocks
    StudySessionService service;

    // ──────────────────────────────────────────────────────────────
    // Fixtures
    // ──────────────────────────────────────────────────────────────
    private Utilisateur buildUser(String id, String username) {
        Utilisateur u = new Utilisateur();
        u.setId(id);
        u.setUsername(username);

        Availability av = new Availability();
        // Use Monday of current week, guaranteed future
        LocalDate monday = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        // If today is Monday, start from now+1h, else full slot
        av.setDay(monday.getDayOfWeek());
        av.setStartTime("00:00");
        av.setEndTime("23:00");
        u.setAvailabilities(List.of(av));
        return u;
    }

    private StudySession buildSession(String id, String userId, String subjectId,
                                      SessionStatus status,
                                      LocalDateTime start, LocalDateTime end) {
        return StudySession.builder()
                .id(id)
                .userId(userId)
                .subjectId(subjectId)
                .status(status)
                .startTime(start)
                .endTime(end)
                .build();
    }

    private Objective buildObjective(String userId, String subjectId,
                                     int weeklyGoal, int progress, int priority) {
        Objective obj = new Objective();
        obj.setUserId(userId);
        obj.setSubjectId(subjectId);
        obj.setWeeklyGoal(weeklyGoal);
        obj.setProgress(progress);
        obj.setPriority(priority);
        obj.setWeekStartDate(LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
        obj.setWeekEndDate(LocalDate.now().with(DayOfWeek.SUNDAY));
        return obj;
    }

    // ──────────────────────────────────────────────────────────────
    // completeSession
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("completeSession")
    class CompleteSession {

        @Test
        @DisplayName(" Session ONGOING → DONE")
        void shouldComplete_whenOngoing() {
            LocalDateTime now = LocalDateTime.now();
            StudySession session = buildSession("s1", "u1", "sub1",
                    SessionStatus.ONGOING, now.minusHours(2), now);

            when(repository.findById("s1")).thenReturn(Optional.of(session));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(objectiveRepository.findByUserId("u1")).thenReturn(List.of());

            StudySession result = service.completeSession("s1", "u1");

            assertThat(result.getStatus()).isEqualTo(SessionStatus.DONE);
            verify(repository).save(session);
        }

        @Test
        @DisplayName(" Session PLANNED → RuntimeException")
        void shouldThrow_whenNotOngoing() {
            LocalDateTime now = LocalDateTime.now();
            StudySession session = buildSession("s1", "u1", "sub1",
                    SessionStatus.PLANNED, now.plusHours(1), now.plusHours(2));

            when(repository.findById("s1")).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> service.completeSession("s1", "u1"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("ONGOING");
        }

        @Test
        @DisplayName(" Mauvais userId → RuntimeException")
        void shouldThrow_whenUnauthorized() {
            LocalDateTime now = LocalDateTime.now();
            StudySession session = buildSession("s1", "owner", "sub1",
                    SessionStatus.ONGOING, now.minusHours(1), now.plusHours(1));

            when(repository.findById("s1")).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> service.completeSession("s1", "hacker"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Unauthorized");
        }

        @Test
        @DisplayName(" Session introuvable → RuntimeException")
        void shouldThrow_whenSessionNotFound() {
            when(repository.findById("999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.completeSession("999", "u1"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // updateObjectiveProgress
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateObjectiveProgress")
    class UpdateObjectiveProgress {

        @Test
        @DisplayName(" Progress incrémenté correctement")
        void shouldIncrementProgress() {
            LocalDate today = LocalDate.now();
            LocalDateTime start = today.atTime(9, 0);
            LocalDateTime end   = today.atTime(11, 0); // 2h

            StudySession session = buildSession("s1", "u1", "sub1",
                    SessionStatus.DONE, start, end);

            Objective obj = buildObjective("u1", "sub1", 10, 0, 1);
            when(objectiveRepository.findByUserId("u1")).thenReturn(List.of(obj));

            service.updateObjectiveProgress(session);

            assertThat(obj.getProgress()).isEqualTo(2);
            verify(objectiveRepository).save(obj);
        }

        @Test
        @DisplayName(" Progress plafonné à weeklyGoal")
        void shouldCapProgressAtGoal() {
            LocalDate today = LocalDate.now();
            LocalDateTime start = today.atTime(8, 0);
            LocalDateTime end   = today.atTime(14, 0); // 6h

            StudySession session = buildSession("s1", "u1", "sub1",
                    SessionStatus.DONE, start, end);

            Objective obj = buildObjective("u1", "sub1", 5, 3, 1);
            when(objectiveRepository.findByUserId("u1")).thenReturn(List.of(obj));

            service.updateObjectiveProgress(session);

            assertThat(obj.getProgress()).isEqualTo(5);
        }

        @Test
        @DisplayName(" Aucun objectif correspondant → rien sauvegardé")
        void shouldDoNothing_whenNoMatchingObjective() {
            LocalDate today = LocalDate.now();
            StudySession session = buildSession("s1", "u1", "otherSub",
                    SessionStatus.DONE, today.atTime(9, 0), today.atTime(11, 0));

            Objective obj = buildObjective("u1", "sub1", 10, 0, 1);
            when(objectiveRepository.findByUserId("u1")).thenReturn(List.of(obj));

            service.updateObjectiveProgress(session);

            verify(objectiveRepository, never()).save(any());
        }
    }

    // ──────────────────────────────────────────────────────────────
    // updateSessionToOngoing
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateSessionToOngoing")
    class UpdateSessionToOngoing {

        @Test
        @DisplayName(" Session PLANNED en cours de temps → ONGOING")
        void shouldMarkOngoing_whenInsideTimeWindow() {
            LocalDateTime now = LocalDateTime.now();
            StudySession session = buildSession("s1", "u1", "sub1",
                    SessionStatus.PLANNED,
                    now.minusMinutes(30),
                    now.plusMinutes(30));

            when(repository.findByUserId("u1")).thenReturn(List.of(session));

            service.updateSessionToOngoing("u1");

            assertThat(session.getStatus()).isEqualTo(SessionStatus.ONGOING);
            verify(repository).saveAll(List.of(session));
        }

        @Test
        @DisplayName(" Session future → reste PLANNED")
        void shouldLeave_whenSessionIsFuture() {
            LocalDateTime now = LocalDateTime.now();
            StudySession session = buildSession("s1", "u1", "sub1",
                    SessionStatus.PLANNED,
                    now.plusHours(2),
                    now.plusHours(4));

            when(repository.findByUserId("u1")).thenReturn(List.of(session));

            service.updateSessionToOngoing("u1");

            assertThat(session.getStatus()).isEqualTo(SessionStatus.PLANNED);
            verify(repository).saveAll(Collections.emptyList());
        }
    }

    // ──────────────────────────────────────────────────────────────
    // calculateFocusStreak
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("calculateFocusStreak")
    class CalculateFocusStreak {

        @Test
        @DisplayName(" Streak de 3 jours consécutifs")
        void shouldReturn3_whenThreeConsecutiveDays() {
            LocalDate today = LocalDate.now();
            StudySession s1 = buildSession("1", "u1", "sub1", SessionStatus.DONE,
                    today.atTime(9, 0), today.atTime(10, 0));
            StudySession s2 = buildSession("2", "u1", "sub1", SessionStatus.DONE,
                    today.minusDays(1).atTime(9, 0), today.minusDays(1).atTime(10, 0));
            StudySession s3 = buildSession("3", "u1", "sub1", SessionStatus.DONE,
                    today.minusDays(2).atTime(9, 0), today.minusDays(2).atTime(10, 0));

            when(repository.findByUserId("u1")).thenReturn(List.of(s1, s2, s3));

            int streak = service.calculateFocusStreak("u1");

            assertThat(streak).isEqualTo(3);
        }

        @Test
        @DisplayName(" Aucune session DONE → streak = 0")
        void shouldReturn0_whenNoSessions() {
            when(repository.findByUserId("u1")).thenReturn(List.of());

            int streak = service.calculateFocusStreak("u1");

            assertThat(streak).isEqualTo(0);
        }

        @Test
        @DisplayName(" Streak cassé → compte seulement jusqu'au trou")
        void shouldCountUntilGap() {
            LocalDate today = LocalDate.now();
            StudySession s1 = buildSession("1", "u1", "sub1", SessionStatus.DONE,
                    today.atTime(9, 0), today.atTime(10, 0));
            // gap: hier absent
            StudySession s3 = buildSession("3", "u1", "sub1", SessionStatus.DONE,
                    today.minusDays(2).atTime(9, 0), today.minusDays(2).atTime(10, 0));

            when(repository.findByUserId("u1")).thenReturn(List.of(s1, s3));

            int streak = service.calculateFocusStreak("u1");

            assertThat(streak).isEqualTo(1);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // getUserSessions
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getUserSessions")
    class GetUserSessions {

        @Test
        @DisplayName(" Retourne les sessions de la semaine avec noms de sujets")
        void shouldReturnSessionsWithSubjectNames() {
            Utilisateur user = buildUser("u1", "alice");
            LocalDate monday = LocalDate.now()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDateTime start = monday.atTime(10, 0);
            LocalDateTime end   = monday.atTime(12, 0);

            StudySession session = buildSession("s1", "u1", "sub1",
                    SessionStatus.DONE, start, end);

            Subject subject = new Subject();
            subject.setId("sub1");
            subject.setName("Math");

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(repository.findByUserIdAndStartTimeBetween(eq("u1"), any(), any()))
                    .thenReturn(List.of(session));
            when(subjectRepository.findById("sub1")).thenReturn(Optional.of(subject));

            List<StudySessionDTO> result = service.getUserSessions("alice", null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSubjectName()).isEqualTo("Math");
        }

        @Test
        @DisplayName(" Sujet introuvable → subjectName = 'Unknown'")
        void shouldReturnUnknown_whenSubjectNotFound() {
            Utilisateur user = buildUser("u1", "alice");
            LocalDate monday = LocalDate.now()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            StudySession session = buildSession("s1", "u1", "sub1",
                    SessionStatus.DONE,
                    monday.atTime(10, 0), monday.atTime(12, 0));

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(repository.findByUserIdAndStartTimeBetween(eq("u1"), any(), any()))
                    .thenReturn(List.of(session));
            when(subjectRepository.findById("sub1")).thenReturn(Optional.empty());

            List<StudySessionDTO> result = service.getUserSessions("alice", null, null);

            assertThat(result.get(0).getSubjectName()).isEqualTo("Unknown");
        }

        @Test
        @DisplayName(" User introuvable → RuntimeException")
        void shouldThrow_whenUserNotFound() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getUserSessions("ghost", null, null))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // generateWeeklyPlan
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("generateWeeklyPlan")
    class GenerateWeeklyPlan {

        @Test
        @DisplayName(" User introuvable → RuntimeException")
        void shouldThrow_whenUserNotFound() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.generateWeeklyPlan("ghost"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName(" Pas de disponibilités → RuntimeException")
        void shouldThrow_whenNoAvailabilities() {
            Utilisateur user = new Utilisateur();
            user.setId("u1");
            user.setUsername("alice");
            user.setAvailabilities(Collections.emptyList());

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> service.generateWeeklyPlan("alice"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("No availability");
        }

        @Test
        @DisplayName(" Sessions DONE préservées, PLANNED supprimées et recréées")
        void shouldPreserveDoneSessions_andDeletePlanned() {
            Utilisateur user = buildUser("u1", "alice");
            LocalDate monday = LocalDate.now()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

            StudySession doneSess = buildSession("done1", "u1", "sub1",
                    SessionStatus.DONE,
                    monday.atTime(8, 0), monday.atTime(9, 0));
            StudySession plannedSess = buildSession("plan1", "u1", "sub1",
                    SessionStatus.PLANNED,
                    monday.atTime(10, 0), monday.atTime(11, 0));

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(repository.findByUserId("u1")).thenReturn(List.of(doneSess, plannedSess));
            when(objectiveRepository.findByUserId("u1")).thenReturn(List.of());
            when(repository.saveAll(any())).thenReturn(List.of());

            service.generateWeeklyPlan("alice");

            verify(repository).deleteAll(List.of(plannedSess));
            verify(notificationService).send(eq("u1"), contains("generated"), anyString(), anyString());
        }
    }
}
