package com.saas.automotriz.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${ALLOWED_ORIGINS:http://localhost:3000,https://www.carvexio.com}")
    private String allowedOrigins;

    private final CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Públicos
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/api/business/search").permitAll()
                .requestMatchers("/api/business/public/**").permitAll()
                .requestMatchers("/api/reviews/business/**").permitAll()
                .requestMatchers("/api/products/search").permitAll()
                .requestMatchers("/api/plans").permitAll()
                .requestMatchers("/api/services/public/**").permitAll()
                .requestMatchers("/api/products/public/**").permitAll()
                .requestMatchers("/api/reclamaciones/**").permitAll()
                .requestMatchers("/api/payments/webhook").permitAll()

                // Roles (Auth0 Permissions)
                .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                .requestMatchers("/api/plans/admin/**").hasAuthority("ADMIN")
                .requestMatchers("/empresa/**").hasAuthority("EMPRESA")

                // CAR-B03: gestión de negocio restringida al rol EMPRESA.
                // (Los GET públicos de catálogo ya están permitidos arriba y se evalúan primero.)
                .requestMatchers("/api/inventory/**").hasAuthority("EMPRESA")
                .requestMatchers("/api/crm/**").hasAuthority("EMPRESA")
                .requestMatchers("/api/reports/**").hasAuthority("EMPRESA")
                .requestMatchers("/api/dashboard/**").hasAuthority("EMPRESA")
                .requestMatchers("/api/branches/me/**").hasAuthority("EMPRESA")
                .requestMatchers("/api/business/me/**").hasAuthority("EMPRESA")
                .requestMatchers("/api/business/documents/**").hasAuthority("EMPRESA")
                .requestMatchers("/api/products/my", "/api/services/my").hasAuthority("EMPRESA")
                // Escritura de catálogo (crear/editar/borrar) solo EMPRESA
                .requestMatchers(HttpMethod.POST, "/api/products/**", "/api/services/**").hasAuthority("EMPRESA")
                .requestMatchers(HttpMethod.PUT, "/api/products/**", "/api/services/**").hasAuthority("EMPRESA")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**", "/api/services/**").hasAuthority("EMPRESA")

                // Todo lo demás requiere login
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(customJwtAuthenticationConverter))
            );

        return http.build();
    }


    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }
}
