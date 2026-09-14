package com.saas.automotriz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class Auth0ManagementService {

    @Value("${auth0.management.client-id:#{null}}")
    private String clientId;

    @Value("${auth0.management.client-secret:#{null}}")
    private String clientSecret;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri; // e.g. https://carvexio-payment.us.auth0.com/

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public void updateUserRoleInAuth0(String email, String newRole) {
        if (clientId == null || clientSecret == null || clientId.isBlank() || clientSecret.isBlank()) {
            log.warn("âš ï¸ Las credenciales de Auth0 Management API no estÃ¡n configuradas (auth0.management.client-id / secret).");
            log.warn("âš ï¸ El rol se cambiÃ³ en la BD local pero NO se sincronizarÃ¡ con Auth0.");
            return;
        }

        try {
            // 1. Obtener Token
            String token = getManagementApiToken();
            if (token == null) return;

            // 2. Buscar usuario por email en Auth0
            String auth0UserId = getAuth0UserIdByEmail(email, token);
            if (auth0UserId == null) {
                log.warn("Usuario con email {} no encontrado en Auth0", email);
                return;
            }

            // 3. Actualizar app_metadata.roles
            updateUserAppMetadata(auth0UserId, token, newRole);

        } catch (Exception e) {
            log.error("Error sincronizando rol en Auth0 para {}: {}", email, e.getMessage());
        }
    }

    private String getManagementApiToken() throws Exception {
        String url = issuerUri + "oauth/token";

        Map<String, String> body = new HashMap<>();
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("audience", issuerUri + "api/v2/");
        body.put("grant_type", "client_credentials");

        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonNode root = objectMapper.readTree(response.body());
            return root.path("access_token").asText();
        } else {
            log.error("Fallo obteniendo token Auth0. HTTP {}", response.statusCode());
        }
        return null;
    }

    private String getAuth0UserIdByEmail(String email, String token) throws Exception {
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        String url = issuerUri + "api/v2/users-by-email?email=" + encodedEmail;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonNode root = objectMapper.readTree(response.body());
            if (root.isArray() && root.size() > 0) {
                return root.get(0).path("user_id").asText();
            }
        } else {
            log.error("Fallo buscando usuario en Auth0. HTTP {}", response.statusCode());
        }
        return null;
    }

    private void updateUserAppMetadata(String auth0UserId, String token, String newRole) throws Exception {
        String url = issuerUri + "api/v2/users/" + auth0UserId;

        // Payload: {"app_metadata": {"roles": ["NUEVO_ROL"]}}
        Map<String, Object> rolesMap = new HashMap<>();
        rolesMap.put("roles", List.of(newRole));
        Map<String, Object> body = new HashMap<>();
        body.put("app_metadata", rolesMap);

        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            log.info("âœ… Rol actualizado exitosamente en Auth0 para {}", auth0UserId);
        } else {
            log.error("Fallo actualizando rol en Auth0. HTTP {}", response.statusCode());
        }
    }
}






