package com.saas.automotriz.controller;

import com.saas.automotriz.dto.ChatResponse;
import com.saas.automotriz.dto.SessionMessageDto;
import com.saas.automotriz.dto.SessionSummaryDto;
import com.saas.automotriz.model.AiChatMessage;
import com.saas.automotriz.model.AiChatSession;
import com.saas.automotriz.model.AiMessageRole;
import com.saas.automotriz.model.User;
import com.saas.automotriz.repository.AiChatMessageRepository;
import com.saas.automotriz.repository.AiChatSessionRepository;
import com.saas.automotriz.request.ChatRequest;
import com.saas.automotriz.service.AiChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiChatService aiChatService;
    private final AiChatSessionRepository sessionRepository;
    private final AiChatMessageRepository messageRepository;

    @PostMapping("/chat")
    @Transactional
    public ResponseEntity<?> chat(@AuthenticationPrincipal User user,
                                  @Valid @RequestBody ChatRequest request) {
        AiChatSession session;
        List<AiChatMessage> history = Collections.emptyList();

        if (request.getSessionPublicId() != null && !request.getSessionPublicId().isEmpty()) {
            UUID sessionId = UUID.fromString(request.getSessionPublicId());
            session = sessionRepository.findByPublicId(sessionId)
                    .orElseThrow(() -> new RuntimeException("Sesión de chat no encontrada"));

            // Seguridad (A01): la sesión debe pertenecer al usuario autenticado
            if (!session.getUser().getId().equals(user.getId())) {
                return ResponseEntity.notFound().build();
            }

            history = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        } else {
            session = new AiChatSession();
            session.setUser(user);
            String title = request.getMessage().length() > 80
                    ? request.getMessage().substring(0, 80) + "..."
                    : request.getMessage();
            session.setTitle(title);
            session = sessionRepository.save(session);
        }

        // Guardar mensaje del usuario
        AiChatMessage userMessage = new AiChatMessage();
        userMessage.setSession(session);
        userMessage.setRole(AiMessageRole.USER);
        userMessage.setContent(request.getMessage());
        messageRepository.save(userMessage);

        // Consultar al asistente
        String aiResponseText = aiChatService.chatWithAi(history, request.getMessage());

        // Guardar respuesta del asistente
        AiChatMessage aiMessage = new AiChatMessage();
        aiMessage.setSession(session);
        aiMessage.setRole(AiMessageRole.ASSISTANT);
        aiMessage.setContent(aiResponseText);
        messageRepository.save(aiMessage);

        return ResponseEntity.ok(new ChatResponse(session.getPublicId().toString(), aiResponseText));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionSummaryDto>> getSessions(@AuthenticationPrincipal User user) {
        List<AiChatSession> sessions = sessionRepository.findSessionsWithMessagesByUserId(user.getId());

        List<SessionSummaryDto> dtos = sessions.stream()
                .map(s -> new SessionSummaryDto(
                        s.getPublicId().toString(),
                        s.getTitle(),
                        s.getMessages().size(),
                        s.getCreatedAt(),
                        s.getUpdatedAt()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/sessions/{sessionPublicId}/messages")
    public ResponseEntity<List<SessionMessageDto>> getSessionMessages(
            @PathVariable UUID sessionPublicId,
            @AuthenticationPrincipal User user) {
        AiChatSession session = sessionRepository.findByPublicId(sessionPublicId)
                .orElseThrow(() -> new RuntimeException("Sesión de chat no encontrada"));

        if (!session.getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        List<AiChatMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());

        List<SessionMessageDto> dtos = messages.stream()
                .map(m -> new SessionMessageDto(
                        m.getRole().name(),
                        m.getContent(),
                        m.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @DeleteMapping("/sessions/{sessionPublicId}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable UUID sessionPublicId,
            @AuthenticationPrincipal User user) {
        AiChatSession session = sessionRepository.findByPublicId(sessionPublicId)
                .orElseThrow(() -> new RuntimeException("Sesión de chat no encontrada"));

        if (!session.getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        // Borrado lógico (soft delete)
        session.setDeletedAt(LocalDateTime.now());
        sessionRepository.save(session);

        return ResponseEntity.noContent().build();
    }
}
