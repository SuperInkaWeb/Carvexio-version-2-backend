package com.saas.automotriz.service;

import com.saas.automotriz.request.RegisterRequest;
import com.saas.automotriz.model.Role;
import com.saas.automotriz.model.User;
import com.saas.automotriz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email ya registrado");
        }

        Role role;

        try {
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Rol inválido");
        }

        // 🚨 Seguridad: NO permitir crear ADMIN desde frontend
        if (role == Role.ADMIN) {
            throw new RuntimeException("No permitido crear ADMIN");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setPhone(request.getPhone());

        return userRepository.save(user);
    }
}