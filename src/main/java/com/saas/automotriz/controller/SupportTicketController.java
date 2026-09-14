package com.saas.automotriz.controller;

import com.saas.automotriz.dto.TicketResponse;
import com.saas.automotriz.model.User;
import com.saas.automotriz.request.CreateTicketRequest;
import com.saas.automotriz.service.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/support/tickets")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@AuthenticationPrincipal User user,
                                                       @Valid @RequestBody CreateTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(supportTicketService.createTicket(user, request));
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> getMyTickets(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(supportTicketService.getMyTickets(user));
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<TicketResponse> getTicket(@AuthenticationPrincipal User user,
                                                    @PathVariable UUID publicId) {
        return ResponseEntity.ok(supportTicketService.getTicket(user, publicId));
    }
}
