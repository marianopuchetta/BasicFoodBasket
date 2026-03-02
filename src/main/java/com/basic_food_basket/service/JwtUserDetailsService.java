package com.basic_food_basket.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;

import com.basic_food_basket.model.Usuario;
import com.basic_food_basket.model.UsuarioDTO;
import com.basic_food_basket.repository.UsuarioRepository;

import java.util.Collections;

@Service
public class JwtUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public JwtUserDetailsService(UsuarioRepository usuarioRepository,
                               @Lazy PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario user = usuarioRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        String role = user.getRole();
        if (role == null || role.isBlank()) {
            role = "ROLE_USER"; // fallback por si hay usuarios viejos sin role seteado
        }

        return new User(
            user.getUsername(),
            user.getPassword(),
            Collections.singletonList(new SimpleGrantedAuthority(role))
        );
    }

    public Usuario save(UsuarioDTO user) {
        Usuario newUser = new Usuario();
        newUser.setUsername(user.getUsername());
        newUser.setPassword(passwordEncoder.encode(user.getPassword()));

        // por defecto USER
        newUser.setRole("ROLE_USER");

        return usuarioRepository.save(newUser);
    }
}