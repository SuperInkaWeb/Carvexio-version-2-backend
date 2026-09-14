package com.saas.automotriz.controller;

import com.saas.automotriz.dto.TransactionDTO;
import com.saas.automotriz.request.TransactionRequest;
import com.saas.automotriz.model.Transaction;
import com.saas.automotriz.model.PaymentStatus;
import com.saas.automotriz.model.User;
import com.saas.automotriz.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository transactionRepository;

    // cliente ve su historial
    @GetMapping
    public ResponseEntity<List<TransactionDTO>> getMyTransactions(@AuthenticationPrincipal User user) {
        List<TransactionDTO> transactions = transactionRepository.findByClient(user)
                .stream().map(this::toDTO).toList();
        return ResponseEntity.ok(transactions);
    }

    // registrar transacción manualmente (se llamará desde MercadoPago webhook después)
    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(@AuthenticationPrincipal User user,
                                                            @RequestBody TransactionRequest request) {
        Transaction transaction = new Transaction();
        transaction.setClient(user);
        transaction.setType(request.getType());
        transaction.setAmount(request.getAmount());
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setReferenceId(request.getReferenceId());
        transaction.setDescription(request.getDescription());
        transaction.setStatus(PaymentStatus.PENDING);
        return ResponseEntity.ok(toDTO(transactionRepository.save(transaction)));
    }

    private TransactionDTO toDTO(Transaction t) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(t.getId());
        dto.setType(t.getType().name());
        dto.setStatus(t.getStatus().name());
        dto.setAmount(t.getAmount());
        dto.setCurrency(t.getCurrency());
        dto.setPaymentMethod(t.getPaymentMethod());
        dto.setDescription(t.getDescription());
        dto.setCreatedAt(t.getCreatedAt());
        return dto;
    }
}