package com.saas.automotriz.security;

import com.saas.automotriz.model.User;
import com.saas.automotriz.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // Extraer email del token (intenta claims estándar o personalizados)
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            email = jwt.getClaimAsString("https://api.carvexio.com/email");
        }

        // Extraer roles del token (como estaba antes en SecurityConfig)
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("https://api.carvexio.com/roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("");
        Collection<GrantedAuthority> authorities = grantedAuthoritiesConverter.convert(jwt);

        if (email == null) {
            // Si por alguna razón el token no tiene email, retorna el token por defecto
            return new UsernamePasswordAuthenticationToken(jwt, jwt, authorities);
        }

        final String finalEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
        final String tokenName = jwt.getClaimAsString("name");

        // Buscar el usuario en la base de datos local o crearlo si no existe
        User user = userRepository.findFirstByEmailIgnoreCase(finalEmail).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(finalEmail);
            // Si el token tiene name, úsalo, sino pon uno por defecto
            newUser.setName(tokenName != null && !tokenName.isBlank() ? tokenName : "Usuario Nuevo");

            // Asignar rol por defecto o extraer del token
            if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ADMIN"))) {
                newUser.setRole(com.saas.automotriz.model.Role.ADMIN);
            } else if (authorities.stream().anyMatch(a -> a.getAuthority().equals("EMPRESA"))) {
                newUser.setRole(com.saas.automotriz.model.Role.EMPRESA);
            } else {
                newUser.setRole(com.saas.automotriz.model.Role.CLIENTE);
            }

            return userRepository.save(newUser);
        });

        // Conserva el nombre que el usuario editó en la plataforma; solo completa perfiles antiguos vacíos.
        if ((user.getName() == null || user.getName().isBlank() || "Usuario Nuevo".equals(user.getName()))
                && tokenName != null && !tokenName.isBlank()) {
            user.setName(tokenName);
            user = userRepository.save(user);
        }

        // Ahora el usuario siempre existe (a menos que falle la BD)
        Object principal = user;

        return new UsernamePasswordAuthenticationToken(principal, jwt, authorities);
    }
}
