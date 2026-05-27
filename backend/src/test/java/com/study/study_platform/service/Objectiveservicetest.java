package com.study.study_platform.service;

import com.study.study_platform.dto.ObjectiveDTO;
import com.study.study_platform.dto.ObjectiveRequest;
import com.study.study_platform.exception.ObjectiveNotFoundException;
import com.study.study_platform.mapper.ObjectiveMapper;
import com.study.study_platform.model.document.Objective;
import com.study.study_platform.model.document.Subject;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.repository.ObjectiveRepository;
import com.study.study_platform.repository.SubjectRepository;
import com.study.study_platform.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ObjectiveService — Tests Unitaires")
class ObjectiveServiceTest {

    @Mock UserRepository userRepository;
    @Mock SubjectRepository subjectRepository;
    @Mock ObjectiveRepository objectiveRepository;
    @Mock ObjectiveMapper objectiveMapper;
    @Mock
    NotificationService notificationService;

    @InjectMocks
    ObjectiveService objectiveService;

    // ──────────────────────────────────────────────────────────────
    // SecurityContext helper
    // ──────────────────────────────────────────────────────────────
    private void mockSecurityContext(String username) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(username);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ──────────────────────────────────────────────────────────────
    // Fixtures
    // ──────────────────────────────────────────────────────────────
    private Utilisateur buildUser(String id, String username) {
        Utilisateur u = new Utilisateur();
        u.setId(id);
        u.setUsername(username);
        return u;
    }

    private Objective buildObjective(String id, String userId, String subjectId,
                                     int goal, int progress) {
        Objective o = new Objective();
        o.setId(id);
        o.setUserId(userId);
        o.setSubjectId(subjectId);
        o.setTitle("Study " + subjectId);
        o.setWeeklyGoal(goal);
        o.setProgress(progress);
        o.setWeekStartDate(LocalDate.now().with(DayOfWeek.MONDAY));
        o.setWeekEndDate(LocalDate.now().with(DayOfWeek.SUNDAY));
        return o;
    }

    private Subject buildSubject(String id, String userId, String name) {
        Subject s = new Subject();
        s.setId(id);
        s.setUserId(userId);
        s.setName(name);
        return s;
    }

    // ──────────────────────────────────────────────────────────────
    // create
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName(" Objectif créé avec dates de semaine auto-calculées")
        void shouldCreate_withAutoWeekDates() {
            mockSecurityContext("alice");

            Utilisateur user = buildUser("u1", "alice");
            Subject subject = buildSubject("sub1", "u1", "Math");
            ObjectiveRequest req = new ObjectiveRequest();
            req.setSubjectId("sub1");
            req.setTitle("Learn Math");
            req.setWeeklyGoal(5);

            Objective entity = buildObjective(null, "u1", "sub1", 5, 0);
            ObjectiveDTO dto = new ObjectiveDTO();
            dto.setSubjectName("Math");

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(subjectRepository.findById("sub1")).thenReturn(Optional.of(subject));
            when(objectiveMapper.toEntity(req, "u1")).thenReturn(entity);
            when(objectiveMapper.toDTO(entity)).thenReturn(dto);
            when(subjectRepository.findAllById(any())).thenReturn(List.of(subject));
            when(objectiveRepository.save(entity)).thenReturn(entity);

            ObjectiveDTO result = objectiveService.create(req);

            assertThat(entity.getWeekStartDate()).isEqualTo(LocalDate.now().with(DayOfWeek.MONDAY));
            assertThat(entity.getWeekEndDate()).isEqualTo(LocalDate.now().with(DayOfWeek.SUNDAY));
            verify(objectiveRepository).save(entity);
        }

        @Test
        @DisplayName(" Subject n'appartient pas à l'utilisateur → RuntimeException")
        void shouldThrow_whenSubjectBelongsToOtherUser() {
            mockSecurityContext("alice");

            Utilisateur user = buildUser("u1", "alice");
            Subject subject = buildSubject("sub1", "other_user", "Math"); // wrong owner
            ObjectiveRequest req = new ObjectiveRequest();
            req.setSubjectId("sub1");

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(subjectRepository.findById("sub1")).thenReturn(Optional.of(subject));

            assertThatThrownBy(() -> objectiveService.create(req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("does not belong");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // getById
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName(" Retourne l'objectif enrichi")
        void shouldReturn_enrichedObjective() {
            mockSecurityContext("alice");

            Utilisateur user = buildUser("u1", "alice");
            Objective obj = buildObjective("obj1", "u1", "sub1", 5, 0);
            Subject subject = buildSubject("sub1", "u1", "Math");
            ObjectiveDTO dto = new ObjectiveDTO();

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(objectiveRepository.findById("obj1")).thenReturn(Optional.of(obj));
            when(objectiveMapper.toDTO(obj)).thenReturn(dto);
            when(subjectRepository.findAllById(any())).thenReturn(List.of(subject));

            ObjectiveDTO result = objectiveService.getById("obj1");

            assertThat(result).isNotNull();
            assertThat(dto.getSubjectName()).isEqualTo("Math");
        }

        @Test
        @DisplayName(" Objectif introuvable → ObjectiveNotFoundException")
        void shouldThrow_whenNotFound() {
            mockSecurityContext("alice");
            Utilisateur user = buildUser("u1", "alice");
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(objectiveRepository.findById("999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> objectiveService.getById("999"))
                    .isInstanceOf(ObjectiveNotFoundException.class);
        }

        @Test
        @DisplayName(" Objectif appartient à un autre utilisateur → RuntimeException")
        void shouldThrow_whenUnauthorized() {
            mockSecurityContext("alice");
            Utilisateur user = buildUser("u1", "alice");
            Objective obj = buildObjective("obj1", "other_user", "sub1", 5, 0);

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(objectiveRepository.findById("obj1")).thenReturn(Optional.of(obj));

            assertThatThrownBy(() -> objectiveService.getById("obj1"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Unauthorized");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // updateProgress
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateProgress")
    class UpdateProgress {

        @Test
        @DisplayName(" Progress mis à jour sans notification si déjà atteint")
        void shouldUpdate_withoutNotification_whenAlreadyCompleted() {
            mockSecurityContext("alice");
            Utilisateur user = buildUser("u1", "alice");
            Objective obj = buildObjective("obj1", "u1", "sub1", 5, 5); // already done
            ObjectiveDTO dto = new ObjectiveDTO();

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(objectiveRepository.findById("obj1")).thenReturn(Optional.of(obj));
            when(objectiveMapper.toDTO(obj)).thenReturn(dto);
            when(subjectRepository.findAllById(any())).thenReturn(List.of());
            when(objectiveRepository.save(obj)).thenReturn(obj);

            objectiveService.updateProgress("obj1", 5);

            verify(notificationService, never()).send(any(), any(), any(), any());
        }

        @Test
        @DisplayName(" Notification envoyée à la première complétion")
        void shouldSendNotification_onFirstCompletion() {
            mockSecurityContext("alice");
            Utilisateur user = buildUser("u1", "alice");
            Objective obj = buildObjective("obj1", "u1", "sub1", 5, 3); // not done yet
            ObjectiveDTO dto = new ObjectiveDTO();

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(objectiveRepository.findById("obj1")).thenReturn(Optional.of(obj));
            when(objectiveMapper.toDTO(obj)).thenReturn(dto);
            when(subjectRepository.findAllById(any())).thenReturn(List.of());
            when(objectiveRepository.save(obj)).thenReturn(obj);

            objectiveService.updateProgress("obj1", 5);

            verify(notificationService).send(eq("u1"), contains("Congratulations"), eq("OBJECTIVE_COMPLETED"), anyString());
        }

        @Test
        @DisplayName(" Objectif introuvable → ObjectiveNotFoundException")
        void shouldThrow_whenNotFound() {
            mockSecurityContext("alice");
            Utilisateur user = buildUser("u1", "alice");
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(objectiveRepository.findById("999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> objectiveService.updateProgress("999", 3))
                    .isInstanceOf(ObjectiveNotFoundException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // update
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName(" Mise à jour du titre et du goal")
        void shouldUpdate_titleAndGoal() {
            mockSecurityContext("alice");
            Utilisateur user = buildUser("u1", "alice");
            Objective obj = buildObjective("obj1", "u1", "sub1", 5, 0);
            ObjectiveRequest req = new ObjectiveRequest();
            req.setTitle("New Title");
            req.setWeeklyGoal(8);
            ObjectiveDTO dto = new ObjectiveDTO();

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(objectiveRepository.findById("obj1")).thenReturn(Optional.of(obj));
            when(objectiveMapper.toDTO(obj)).thenReturn(dto);
            when(subjectRepository.findAllById(any())).thenReturn(List.of());
            when(objectiveRepository.save(obj)).thenReturn(obj);

            objectiveService.update("obj1", req);

            assertThat(obj.getTitle()).isEqualTo("New Title");
            assertThat(obj.getWeeklyGoal()).isEqualTo(8);
        }

        @Test
        @DisplayName(" Accès refusé → RuntimeException")
        void shouldThrow_whenUnauthorized() {
            mockSecurityContext("alice");
            Utilisateur user = buildUser("u1", "alice");
            Objective obj = buildObjective("obj1", "other_user", "sub1", 5, 0);
            ObjectiveRequest req = new ObjectiveRequest();

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(objectiveRepository.findById("obj1")).thenReturn(Optional.of(obj));

            assertThatThrownBy(() -> objectiveService.update("obj1", req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Unauthorized");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // delete
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName(" Suppression réussie")
        void shouldDelete_whenAuthorized() {
            mockSecurityContext("alice");
            Utilisateur user = buildUser("u1", "alice");
            Objective obj = buildObjective("obj1", "u1", "sub1", 5, 0);

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(objectiveRepository.findById("obj1")).thenReturn(Optional.of(obj));

            objectiveService.delete("obj1");

            verify(objectiveRepository).delete(obj);
        }

        @Test
        @DisplayName(" Non autorisé → RuntimeException")
        void shouldThrow_whenUnauthorized() {
            mockSecurityContext("alice");
            Utilisateur user = buildUser("u1", "alice");
            Objective obj = buildObjective("obj1", "other_user", "sub1", 5, 0);

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(objectiveRepository.findById("obj1")).thenReturn(Optional.of(obj));

            assertThatThrownBy(() -> objectiveService.delete("obj1"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Unauthorized");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // stats
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("stats — getTotalObjectives / getAchievedCount / getPendingCount")
    class Stats {

        @Test
        @DisplayName(" Comptage total, atteint, en attente")
        void shouldReturnCorrectCounts() {
            mockSecurityContext("alice");
            Utilisateur user = buildUser("u1", "alice");
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

            Objective done = buildObjective("1", "u1", "sub1", 5, 5);
            Objective pending = buildObjective("2", "u1", "sub2", 5, 2);

            when(objectiveRepository.findByUserId("u1")).thenReturn(List.of(done, pending));

            // Need fresh stubbing per call since same mock is shared
            assertThat(objectiveService.getTotalObjectives()).isEqualTo(2);
        }

        @Test
        @DisplayName(" getAchievedCount retourne le bon nombre")
        void shouldReturnAchievedCount() {
            mockSecurityContext("alice");
            Utilisateur user = buildUser("u1", "alice");
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

            Objective done = buildObjective("1", "u1", "sub1", 5, 5);
            Objective pending = buildObjective("2", "u1", "sub2", 5, 2);
            when(objectiveRepository.findByUserId("u1")).thenReturn(List.of(done, pending));

            assertThat(objectiveService.getAchievedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName(" getPendingCount retourne le bon nombre")
        void shouldReturnPendingCount() {
            mockSecurityContext("alice");
            Utilisateur user = buildUser("u1", "alice");
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

            Objective done = buildObjective("1", "u1", "sub1", 5, 5);
            Objective pending = buildObjective("2", "u1", "sub2", 5, 2);
            when(objectiveRepository.findByUserId("u1")).thenReturn(List.of(done, pending));

            assertThat(objectiveService.getPendingCount()).isEqualTo(1);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // getHistory
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getHistory")
    class GetHistory {

        @Test
        @DisplayName(" Retourne uniquement les objectifs dont weekEndDate est passé")
        void shouldReturnPastObjectives() {
            mockSecurityContext("alice");
            Utilisateur user = buildUser("u1", "alice");

            Objective past = buildObjective("1", "u1", "sub1", 5, 5);
            past.setWeekEndDate(LocalDate.now().minusDays(7));

            Objective current = buildObjective("2", "u1", "sub2", 5, 2);
            current.setWeekEndDate(LocalDate.now().with(DayOfWeek.SUNDAY));

            ObjectiveDTO dto = new ObjectiveDTO();

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(objectiveRepository.findByUserId("u1")).thenReturn(List.of(past, current));
            when(objectiveMapper.toDTO(past)).thenReturn(dto);
            when(subjectRepository.findAllById(any())).thenReturn(List.of());

            List<ObjectiveDTO> result = objectiveService.getHistory();

            assertThat(result).hasSize(1);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // getGlobalStats (admin)
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getGlobalStats")
    class GetGlobalStats {

        @Test
        @DisplayName(" Calcule correctement taux de complétion")
        void shouldComputeCompletionRate() {
            Objective done = buildObjective("1", "u1", "sub1", 5, 5);
            Objective pending = buildObjective("2", "u2", "sub2", 5, 2);

            when(objectiveRepository.findAll()).thenReturn(List.of(done, pending));

            Map<String, Object> stats = objectiveService.getGlobalStats();

            assertThat(stats.get("totalObjectives")).isEqualTo(2L);
            assertThat(stats.get("achievedObjectives")).isEqualTo(1L);
            assertThat((Double) stats.get("completionRate")).isEqualTo(50.0);
        }

        @Test
        @DisplayName(" Aucun objectif → taux = 0")
        void shouldReturn0Rate_whenNoObjectives() {
            when(objectiveRepository.findAll()).thenReturn(List.of());

            Map<String, Object> stats = objectiveService.getGlobalStats();

            assertThat(stats.get("completionRate")).isEqualTo(0.0);
        }
    }
}
