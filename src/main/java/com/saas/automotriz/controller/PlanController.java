package com.saas.automotriz.controller;

import com.saas.automotriz.dto.PlanDTO;
import com.saas.automotriz.model.Business;
import com.saas.automotriz.model.Plan;
import com.saas.automotriz.model.User;
import com.saas.automotriz.repository.BusinessRepository;
import com.saas.automotriz.repository.PlanRepository;
import com.saas.automotriz.request.PlanRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanRepository planRepository;
    private final BusinessRepository businessRepository;

    // público - cualquiera puede ver los planes disponibles
    @GetMapping
    public ResponseEntity<List<PlanDTO>> getPlans() {
        List<PlanDTO> plans = planRepository.findByActiveTrue()
                .stream().map(this::toDTO).toList();
        return ResponseEntity.ok(plans);
    }

    // admin crea plan
    @PostMapping("/admin")
    public ResponseEntity<PlanDTO> createPlan(@RequestBody PlanRequest request) {
        Plan plan = new Plan();
        applyRequest(plan, request);
        return ResponseEntity.ok(toDTO(planRepository.save(plan)));
    }

    // admin edita plan
    @PutMapping("/admin/{id}")
    public ResponseEntity<PlanDTO> updatePlan(@PathVariable Long id,
                                              @RequestBody PlanRequest request) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado"));
        applyRequest(plan, request);
        return ResponseEntity.ok(toDTO(planRepository.save(plan)));
    }

    // admin elimina plan
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado"));
        plan.setActive(false);
        planRepository.save(plan);
        return ResponseEntity.noContent().build();
    }

    // negocio se suscribe directo a un plan gratuito (los planes pagados solo se asignan
    // via webhook de Mercado Pago, despues de confirmarse el pago real)
    @PutMapping("/{planId}/subscribe")
    public ResponseEntity<Void> subscribeToPlan(@AuthenticationPrincipal User user,
                                                @PathVariable Long planId) {
        Business business = businessRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado"));
        if (plan.getPrice() != null && plan.getPrice() > 0) {
            return ResponseEntity.status(402).build(); // Payment Required: debe pagar via Mercado Pago
        }
        business.setPlan(plan);
        businessRepository.save(business);
        return ResponseEntity.noContent().build();
    }

    private void applyRequest(Plan plan, PlanRequest r) {
        if (r.getName() != null) plan.setName(r.getName());
        if (r.getDescription() != null) plan.setDescription(r.getDescription());
        if (r.getPrice() != null) plan.setPrice(r.getPrice());
        if (r.getCommission() != null) plan.setCommission(r.getCommission());
        if (r.getHasMarketplace() != null) plan.setHasMarketplace(r.getHasMarketplace());
        if (r.getHasCrm() != null) plan.setHasCrm(r.getHasCrm());
        if (r.getHasInventory() != null) plan.setHasInventory(r.getHasInventory());
        if (r.getHasReports() != null) plan.setHasReports(r.getHasReports());
        if (r.getHasWhatsapp() != null) plan.setHasWhatsapp(r.getHasWhatsapp());
    }

    private PlanDTO toDTO(Plan p) {
        PlanDTO dto = new PlanDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setDescription(p.getDescription());
        dto.setPrice(p.getPrice());
        dto.setCommission(p.getCommission());
        dto.setHasMarketplace(p.getHasMarketplace());
        dto.setHasCrm(p.getHasCrm());
        dto.setHasInventory(p.getHasInventory());
        dto.setHasReports(p.getHasReports());
        dto.setHasWhatsapp(p.getHasWhatsapp());
        return dto;
    }
}