package com.saas.automotriz.controller;

import com.saas.automotriz.request.LoginRequest;
import com.saas.automotriz.request.RegisterRequest;
import com.saas.automotriz.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.saas.automotriz.model.User; // âœ… tu User con getId()
import org.springframework.security.authentication.*;
import org.springframework.web.bind.annotation.*;

import com.saas.automotriz.dto.*;
import com.saas.automotriz.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Map;

//@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtils jwtUtils;
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            System.out.println("ðŸ” Intentando login con: " + request.getEmail());
            System.out.println("ðŸ”‘ Password recibido: " + request.getPassword());

            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
            System.out.println("ðŸ‘¤ Usuario encontrado: " + userDetails.getUsername());
            System.out.println("ðŸ”’ Hash en BD: " + userDetails.getPassword());

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()));

            String token = jwtUtils.generateToken(userDetails);
            return ResponseEntity.ok(new AuthResponse(token));

        } catch (BadCredentialsException e) {
            System.out.println("âŒ Credenciales incorrectas");
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Credenciales incorrectas"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = authService.register(request);
            return ResponseEntity.ok(Map.of(
                    "message", "Usuario registrado con Ã©xito",
                    "userId", user.getId()
            ));
        } catch (RuntimeException e) {
            log.warn("Registro rechazado: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", "No se pudo completar el registro"));
        }
    }
}


