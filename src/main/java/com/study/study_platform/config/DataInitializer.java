package com.study.study_platform.config;


import com.study.study_platform.model.document.Role;
import com.study.study_platform.model.document.Utilisateur;
import com.study.study_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (utilisateurRepository.findByUsername("admin").isEmpty()) {
            Utilisateur admin = Utilisateur.builder()
                    .name("Admin Super")
                    .username("admin")
                    .email("admin@test.com")
                    .password(passwordEncoder.encode("ChangeMe123!"))
                    .role(Role.ADMIN)
                    .build();
            utilisateurRepository.save(admin);
            System.out.println("Admin root créé avec login 'admin' et mot de passe 'ChangeMe123!'");
        }
    }

}

