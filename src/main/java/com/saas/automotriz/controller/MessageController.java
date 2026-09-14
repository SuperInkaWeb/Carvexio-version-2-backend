package com.saas.automotriz.controller;

import com.saas.automotriz.dto.DirectMessageDTO;
import com.saas.automotriz.dto.MessageContactDTO;
import com.saas.automotriz.model.*;
import com.saas.automotriz.repository.*;
import com.saas.automotriz.request.DirectMessageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final BusinessRepository businessRepository;
    private final OrderRepository orderRepository;
    private final BookingRepository bookingRepository;
    private final DirectMessageRepository messageRepository;

    @GetMapping("/contacts")
    public ResponseEntity<List<MessageContactDTO>> getContacts(@AuthenticationPrincipal User user,
                                                                @RequestParam(defaultValue = "client") String mode) {
        Optional<Business> ownedBusiness = businessRepository.findByOwnerId(user.getId());
        if (isBusinessMode(mode) && ownedBusiness.isPresent()) {
            Business business = ownedBusiness.get();
            Set<User> clients = new LinkedHashSet<>(orderRepository.findClientsByBusiness(business));
            clients.addAll(bookingRepository.findClientsByBusiness(business));
            clients.addAll(messageRepository.findDistinctClientsByBusiness(business));
            return ResponseEntity.ok(clients.stream().map(client -> toClientContact(business, client)).toList());
        }

        Set<Business> businesses = new LinkedHashSet<>();
        orderRepository.findByClientOrderByCreatedAtDesc(user).forEach(order -> businesses.add(order.getBusiness()));
        bookingRepository.findByClient(user).forEach(booking -> businesses.add(booking.getBusiness()));
        return ResponseEntity.ok(businesses.stream().filter(Objects::nonNull).map(business -> toBusinessContact(user, business)).toList());
    }

    @GetMapping("/conversation")
    @Transactional
    public ResponseEntity<List<DirectMessageDTO>> getConversation(@AuthenticationPrincipal User user,
                                                                    @RequestParam Long businessId,
                                                                    @RequestParam(required = false) Long clientId,
                                                                    @RequestParam(defaultValue = "client") String mode) {
        Business business = findBusiness(businessId);
        User client = resolveConversationClient(user, business, clientId, isBusinessMode(mode));
        List<DirectMessage> messages = messageRepository.findByBusinessAndClientOrderByCreatedAtAsc(business, client);
        boolean businessMode = isBusinessMode(mode);
        messages.stream()
                .filter(message -> !message.isRead() && message.isSentByBusiness() != businessMode)
                .forEach(message -> message.setRead(true));
        return ResponseEntity.ok(messages.stream().map(message -> toMessageDTO(message, businessMode)).toList());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<DirectMessageDTO> sendMessage(@AuthenticationPrincipal User user,
                                                         @RequestBody DirectMessageRequest request,
                                                         @RequestParam(defaultValue = "client") String mode) {
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (request.getContent().trim().length() > 2000) {
            return ResponseEntity.badRequest().build();
        }

        Business business = findBusiness(request.getBusinessId());
        User client = resolveConversationClient(user, business, request.getClientId(), isBusinessMode(mode));

        DirectMessage message = new DirectMessage();
        message.setBusiness(business);
        message.setClient(client);
        message.setSender(user);
        message.setContent(request.getContent().trim());
        message.setRead(false);
        boolean businessMode = isBusinessMode(mode);
        message.setSentByBusiness(businessMode);
        return ResponseEntity.ok(toMessageDTO(messageRepository.save(message), businessMode));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal User user,
                                                             @RequestParam(defaultValue = "client") String mode) {
        long count = isBusinessMode(mode) ? businessRepository.findByOwnerId(user.getId())
                .map(messageRepository::countByBusinessAndSentByBusinessFalseAndReadFalse)
                .orElse(0L)
                : messageRepository.countByClientAndSentByBusinessTrueAndReadFalse(user);
        return ResponseEntity.ok(Map.of("count", count));
    }

    private Business findBusiness(Long businessId) {
        if (businessId == null) throw new IllegalArgumentException("Negocio requerido");
        return businessRepository.findById(businessId)
                .orElseThrow(() -> new NoSuchElementException("Negocio no encontrado"));
    }

    private User resolveConversationClient(User user, Business business, Long clientId, boolean businessUser) {
        if (!businessUser) {
            if (!hasRelationship(user, business)) {
                throw new SecurityException("Solo puedes escribir a negocios donde tienes una compra o reserva");
            }
            return user;
        }
        if (clientId == null) throw new IllegalArgumentException("Cliente requerido");
        User client = orderRepository.findClientsByBusiness(business).stream()
                .filter(item -> item.getId().equals(clientId))
                .findFirst()
                .orElseGet(() -> bookingRepository.findClientsByBusiness(business).stream()
                        .filter(item -> item.getId().equals(clientId))
                        .findFirst()
                        .orElseThrow(() -> new SecurityException("El cliente no pertenece a este negocio")));
        return client;
    }

    private boolean hasRelationship(User client, Business business) {
        return orderRepository.findByBusinessAndClient(business, client).size() > 0
                || bookingRepository.findByBusinessAndClient(business, client).size() > 0;
    }

    private boolean isBusinessMode(String mode) {
        return "business".equalsIgnoreCase(mode);
    }

    private MessageContactDTO toBusinessContact(User user, Business business) {
        MessageContactDTO dto = new MessageContactDTO();
        dto.setBusinessId(business.getId());
        dto.setName(business.getName());
        dto.setImageUrl(business.getLogoUrl());
        dto.setUnreadCount(messageRepository.countByBusinessAndClientAndSentByBusinessTrueAndReadFalse(business, user));
        return dto;
    }

    private MessageContactDTO toClientContact(Business business, User client) {
        MessageContactDTO dto = new MessageContactDTO();
        dto.setBusinessId(business.getId());
        dto.setClientId(client.getId());
        dto.setName(client.getName() == null || client.getName().isBlank() ? client.getEmail() : client.getName());
        dto.setUnreadCount(messageRepository.countByBusinessAndClientAndSentByBusinessFalseAndReadFalse(business, client));
        return dto;
    }

    private DirectMessageDTO toMessageDTO(DirectMessage message, boolean businessMode) {
        DirectMessageDTO dto = new DirectMessageDTO();
        dto.setId(message.getId());
        dto.setContent(message.getContent());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setMine(message.isSentByBusiness() == businessMode);
        return dto;
    }
}
