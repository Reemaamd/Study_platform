package com.study.study_platform.security;


//import com.studyplatform.model.document.Role;
import com.study.study_platform.model.document.Utilisateur;
import org.springframework.security.core.userdetails.User;
//import com.studyplatform.exception.CabinetDesactiveException;
import com.study.study_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository utilisateurRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec login : " + username));


        // Récupérer le rôle et construire les authorities
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + utilisateur.getRole().name()));

        authorities.forEach(a -> System.out.println("AUTHORITY LOADED = " + a.getAuthority()));


        // Retourner un UserDetails Spring Security
        return new User(utilisateur.getUsername(), utilisateur.getPassword() , authorities);
    }
}

