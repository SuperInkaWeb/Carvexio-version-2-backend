package com.saas.automotriz.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting en memoria (ventana fija por IP) para endpoints sensibles, sin
 * dependencias externas. Mitiga abuso de recursos: spam del libro de
 * reclamaciones, costo del asistente de IA y scraping de las busquedas publicas.
 *
 * Limitacion conocida: el estado es por instancia; en despliegues multi-replica
 * conviene migrar a un limitador distribuido (Redis / Bucket4j + Lettuce).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = Duration.ofMinutes(1).toMillis();
    private static final int MAX_TRACKED_KEYS = 50_000;

    // Limite de solicitudes por minuto segun el prefijo de ruta.
    private static final Map<String, Integer> LIMITS = Map.of(
            "/api/reclamaciones", 5,
            "/api/ai/chat", 15,
            "/api/business/search", 60,
            "/api/products/search", 60
    );

    private final ConcurrentHashMap<String, Window> counters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String matchedPath = matchPath(request.getRequestURI());
        if (matchedPath != null) {
            int limit = LIMITS.get(matchedPath);
            String key = matchedPath + '|' + clientIp(request);
            if (!allow(key, limit)) {
                response.setStatus(429);
                response.setHeader("Retry-After", String.valueOf(WINDOW_MS / 1000));
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\":\"Demasiadas solicitudes. Intenta de nuevo en un minuto.\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private String matchPath(String uri) {
        for (String prefix : LIMITS.keySet()) {
            if (uri.equals(prefix) || uri.startsWith(prefix + "/")) {
                return prefix;
            }
        }
        return null;
    }

    private boolean allow(String key, int limit) {
        long now = System.currentTimeMillis();
        if (counters.size() > MAX_TRACKED_KEYS) {
            counters.values().removeIf(w -> now - w.start >= WINDOW_MS);
        }
        Window window = counters.compute(key, (k, existing) ->
                (existing == null || now - existing.start >= WINDOW_MS) ? new Window(now) : existing);
        return window.count.incrementAndGet() <= limit;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Window {
        final long start;
        final AtomicInteger count = new AtomicInteger(0);

        Window(long start) {
            this.start = start;
        }
    }
}
