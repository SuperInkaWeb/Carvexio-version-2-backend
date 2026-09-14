package com.saas.automotriz.service;

import com.saas.automotriz.dto.TicketResponse;
import com.saas.automotriz.model.SupportTicket;
import com.saas.automotriz.model.User;
import com.saas.automotriz.repository.SupportTicketRepository;
import com.saas.automotriz.request.CreateTicketRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Canal de soporte del negocio: permite al dueño abrir tickets a la plataforma,
 * de forma separada al chat con sus clientes.
 */
@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;

    @Transactional
    public TicketResponse createTicket(User user, CreateTicketRequest request) {
        SupportTicket ticket = new SupportTicket();
        ticket.setUser(user);
        ticket.setSubject(request.getSubject());
        ticket.setDescription(request.getDescription());
        ticket.setCategory(request.getCategory());
        // Prioridad por defecto NORMAL (el soporte no está detrás de un plan de pago).

        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getMyTickets(User user) {
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicket(User user, UUID publicId) {
        SupportTicket ticket = ticketRepository.findByPublicId(publicId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        // Seguridad (A01): el ticket debe pertenecer al usuario autenticado.
        if (!ticket.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Ticket no encontrado");
        }
        return toResponse(ticket);
    }

    private TicketResponse toResponse(SupportTicket t) {
        return new TicketResponse(
                t.getPublicId(),
                t.getSubject(),
                t.getDescription(),
                t.getStatus(),
                t.getPriority(),
                t.getCategory(),
                t.getCreatedAt()
        );
    }
}
