package com.study.study_platform.service;

import com.study.study_platform.dto.UserRequest;
import com.study.study_platform.dto.UserResponse;
import com.study.study_platform.exception.BadRequestException;
import com.study.study_platform.exception.ResourceNotFoundException;
import com.study.study_platform.exception.UserAlreadyExistsException;
import com.study.study_platform.mapper.UserMapper;
import com.study.study_platform.model.document.Role;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService — Tests Unitaires")
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock UserMapper userMapper;

    @InjectMocks
    UserService userService;

    // ──────────────────────────────────────────────────────────────
    // Fixtures
    // ──────────────────────────────────────────────────────────────
    private Utilisateur buildUser(String id, String username, String email) {
        Utilisateur u = new Utilisateur();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(email);
        u.setPassword("encoded");
        u.setRole(Role.USER);
        return u;
    }

    private UserRequest buildRequest(String username, String email, String password) {
        UserRequest req = new UserRequest();
        req.setUsername(username);
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    // ──────────────────────────────────────────────────────────────
    // createUtilisateur
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("createUtilisateur")
    class CreateUtilisateur {

        @Test
        @DisplayName(" Succès — utilisateur créé correctement")
        void shouldCreateUser_whenUsernameAndEmailAreUnique() {
            UserRequest req = buildRequest("alice", "alice@test.com", "pass123");
            Utilisateur mapped = buildUser(null, "alice", "alice@test.com");

            when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.empty());
            when(userMapper.toEntity(req)).thenReturn(mapped);
            when(passwordEncoder.encode("pass123")).thenReturn("encoded_pass");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Utilisateur result = userService.createUtilisateur(req);

            assertThat(result.getUsername()).isEqualTo("alice");
            assertThat(result.getPassword()).isEqualTo("encoded_pass");
            assertThat(result.isPasswordMustChange()).isTrue();
            verify(userRepository).save(mapped);
        }

        @Test
        @DisplayName(" Username déjà pris → UserAlreadyExistsException")
        void shouldThrow_whenUsernameAlreadyExists() {
            UserRequest req = buildRequest("alice", "alice@test.com", "pass");
            when(userRepository.findByUsername("alice"))
                    .thenReturn(Optional.of(buildUser("1", "alice", "other@test.com")));

            assertThatThrownBy(() -> userService.createUtilisateur(req))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("Username already exists");
        }

        @Test
        @DisplayName(" Email déjà pris → UserAlreadyExistsException")
        void shouldThrow_whenEmailAlreadyExists() {
            UserRequest req = buildRequest("newuser", "alice@test.com", "pass");
            when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("alice@test.com"))
                    .thenReturn(Optional.of(buildUser("1", "alice", "alice@test.com")));

            assertThatThrownBy(() -> userService.createUtilisateur(req))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("Email already exists");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // getAllUtilisateurs
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getAllUtilisateurs")
    class GetAllUtilisateurs {

        @Test
        @DisplayName(" Retourne les utilisateurs non-ADMIN")
        void shouldReturnNonAdminUsers() {
            List<Utilisateur> users = List.of(
                    buildUser("1", "alice", "a@test.com"),
                    buildUser("2", "bob", "b@test.com")
            );
            when(userRepository.findByRoleNot(Role.ADMIN)).thenReturn(users);

            List<Utilisateur> result = userService.getAllUtilisateurs();

            assertThat(result).hasSize(2);
            verify(userRepository).findByRoleNot(Role.ADMIN);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // getUtilisateurById
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getUtilisateurById")
    class GetUtilisateurById {

        @Test
        @DisplayName(" Retourne l'utilisateur existant")
        void shouldReturnUser_whenFound() {
            Utilisateur user = buildUser("1", "alice", "a@test.com");
            when(userRepository.findById("1")).thenReturn(Optional.of(user));

            Utilisateur result = userService.getUtilisateurById("1");

            assertThat(result.getUsername()).isEqualTo("alice");
        }

        @Test
        @DisplayName(" ID introuvable → ResourceNotFoundException")
        void shouldThrow_whenNotFound() {
            when(userRepository.findById("999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUtilisateurById("999"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // updateUtilisateur
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateUtilisateur")
    class UpdateUtilisateur {

        @Test
        @DisplayName(" Mise à jour réussie sans changement de username/email")
        void shouldUpdateUser_whenNoConflict() {
            Utilisateur existing = buildUser("1", "alice", "alice@test.com");
            UserRequest req = buildRequest("alice", "alice@test.com", null);

            when(userRepository.findById("1")).thenReturn(Optional.of(existing));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Utilisateur result = userService.updateUtilisateur("1", req);

            verify(userMapper).updateEntity(existing, req);
            verify(userRepository).save(existing);
        }

        @Test
        @DisplayName(" Mot de passe mis à jour si fourni")
        void shouldEncodePassword_whenPasswordProvided() {
            Utilisateur existing = buildUser("1", "alice", "alice@test.com");
            UserRequest req = buildRequest("alice", "alice@test.com", "newPass");

            when(userRepository.findById("1")).thenReturn(Optional.of(existing));
            when(passwordEncoder.encode("newPass")).thenReturn("encoded_new");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            userService.updateUtilisateur("1", req);

            assertThat(existing.getPassword()).isEqualTo("encoded_new");
        }

        @Test
        @DisplayName(" Nouveau username déjà pris → RuntimeException")
        void shouldThrow_whenNewUsernameConflicts() {
            Utilisateur existing = buildUser("1", "alice", "alice@test.com");
            UserRequest req = buildRequest("bob", "alice@test.com", null);

            when(userRepository.findById("1")).thenReturn(Optional.of(existing));
            when(userRepository.findByUsername("bob"))
                    .thenReturn(Optional.of(buildUser("2", "bob", "bob@test.com")));

            assertThatThrownBy(() -> userService.updateUtilisateur("1", req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Username already exists");
        }

        @Test
        @DisplayName(" Nouvel email déjà pris → RuntimeException")
        void shouldThrow_whenNewEmailConflicts() {
            Utilisateur existing = buildUser("1", "alice", "alice@test.com");
            UserRequest req = buildRequest("alice", "bob@test.com", null);

            when(userRepository.findById("1")).thenReturn(Optional.of(existing));
            when(userRepository.findByEmail("bob@test.com"))
                    .thenReturn(Optional.of(buildUser("2", "bob", "bob@test.com")));

            assertThatThrownBy(() -> userService.updateUtilisateur("1", req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Email already exists");
        }
    }

    // ──────────────────────────────────────────────────────────────
    // deleteUtilisateur
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("deleteUtilisateur")
    class DeleteUtilisateur {

        @Test
        @DisplayName(" Suppression réussie")
        void shouldDelete_whenUserExists() {
            Utilisateur user = buildUser("1", "alice", "a@test.com");
            when(userRepository.findById("1")).thenReturn(Optional.of(user));

            userService.deleteUtilisateur("1");

            verify(userRepository).delete(user);
        }

        @Test
        @DisplayName(" ID introuvable → ResourceNotFoundException")
        void shouldThrow_whenUserNotFound() {
            when(userRepository.findById("999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUtilisateur("999"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // changePassword
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName(" Changement de mot de passe réussi")
        void shouldChangePassword_whenOldPasswordMatches() {
            Utilisateur user = buildUser("1", "alice", "a@test.com");
            user.setPasswordMustChange(true);

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("oldPass", "encoded")).thenReturn(true);
            when(passwordEncoder.encode("newPass")).thenReturn("encoded_new");

            userService.changePassword("alice", "oldPass", "newPass");

            assertThat(user.getPassword()).isEqualTo("encoded_new");
            assertThat(user.isPasswordMustChange()).isFalse();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName(" Ancien mot de passe incorrect → BadRequestException")
        void shouldThrow_whenOldPasswordWrong() {
            Utilisateur user = buildUser("1", "alice", "a@test.com");
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongPass", "encoded")).thenReturn(false);

            assertThatThrownBy(() -> userService.changePassword("alice", "wrongPass", "newPass"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Ancien mot de passe incorrect");
        }

        @Test
        @DisplayName(" Utilisateur introuvable → ResourceNotFoundException")
        void shouldThrow_whenUserNotFound() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.changePassword("ghost", "old", "new"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // getCurrentUser
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getCurrentUser")
    class GetCurrentUser {

        @Test
        @DisplayName(" Retourne le UserResponse via le mapper")
        void shouldReturnUserResponse() {
            Utilisateur user = buildUser("1", "alice", "a@test.com");
            UserResponse response = new UserResponse();

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(userMapper.toResponse(user)).thenReturn(response);

            UserResponse result = userService.getCurrentUser("alice");

            assertThat(result).isSameAs(response);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // updateByUsername / deleteByUsername
    // ──────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updateByUsername / deleteByUsername")
    class ByUsername {

        @Test
        @DisplayName(" updateByUsername — mise à jour réussie")
        void shouldUpdate_byUsername() {
            Utilisateur user = buildUser("1", "alice", "a@test.com");
            UserRequest req = buildRequest("alice", "a@test.com", null);

            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Utilisateur result = userService.updateByUsername("alice", req);

            verify(userMapper).updateEntity(user, req);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName(" deleteByUsername — suppression réussie")
        void shouldDelete_byUsername() {
            Utilisateur user = buildUser("1", "alice", "a@test.com");
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

            userService.deleteByUsername("alice");

            verify(userRepository).delete(user);
        }

        @Test
        @DisplayName(" deleteByUsername — utilisateur introuvable")
        void shouldThrow_deleteByUsername_whenNotFound() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteByUsername("ghost"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
