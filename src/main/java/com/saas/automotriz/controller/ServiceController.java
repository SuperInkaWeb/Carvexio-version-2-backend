package com.saas.automotriz.controller;

import com.saas.automotriz.dto.ServiceDTO;
import com.saas.automotriz.request.ServiceRequest;
import com.saas.automotriz.model.AutoService;
import com.saas.automotriz.model.Branch;
import com.saas.automotriz.model.Business;
import com.saas.automotriz.model.User;
import com.saas.automotriz.repository.BranchRepository;
import com.saas.automotriz.repository.BusinessRepository;
import com.saas.automotriz.repository.ServiceRepository;
import com.saas.automotriz.service.PlanLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceRepository serviceRepository;
    private final BusinessRepository businessRepository;
    private final BranchRepository branchRepository;
    private final PlanLimitService planLimitService;

    @GetMapping
    public ResponseEntity<List<ServiceDTO>> getMyServices(@AuthenticationPrincipal User user,
                                                          @RequestParam(required = false) Long localId) {
        Business business = businessRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));
        List<AutoService> list;
        if (localId != null) {
            list = serviceRepository.findByBusinessAndBranchIdAndActiveTrue(business, localId);
        } else {
            list = serviceRepository.findByBusinessAndActiveTrue(business);
        }
        List<ServiceDTO> services = list.stream().map(this::toDTO).toList();
        return ResponseEntity.ok(services);
    }

    @PostMapping
    public ResponseEntity<ServiceDTO> createService(@AuthenticationPrincipal User user,
                                                    @RequestBody ServiceRequest request) {
        Business business = businessRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));
        planLimitService.checkServiceLimit(business, serviceRepository.countByBusinessAndActiveTrue(business), 1);
        AutoService service = new AutoService();
        service.setBusiness(business);
        if (request.getLocalId() != null) {
            branchRepository.findById(request.getLocalId()).ifPresent(service::setBranch);
        }
        applyRequest(service, request);
        return ResponseEntity.ok(toDTO(serviceRepository.save(service)));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<ServiceDTO>> createServicesBulk(@AuthenticationPrincipal User user,
                                                              @RequestBody List<ServiceRequest> requests) {
        Business business = businessRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));
        planLimitService.checkServiceLimit(business, serviceRepository.countByBusinessAndActiveTrue(business), requests.size());
        List<AutoService> services = requests.stream().map(request -> {
            AutoService service = new AutoService();
            service.setBusiness(business);
            if (request.getLocalId() != null) {
                branchRepository.findById(request.getLocalId()).ifPresent(service::setBranch);
            }
            applyRequest(service, request);
            return service;
        }).toList();
        List<AutoService> saved = serviceRepository.saveAll(services);
        return ResponseEntity.ok(saved.stream().map(this::toDTO).toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceDTO> updateService(@AuthenticationPrincipal User user,
                                                    @PathVariable Long id,
                                                    @RequestBody ServiceRequest request) {
        AutoService service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));

        if (!service.getBusiness().getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        if (request.getLocalId() != null) {
            branchRepository.findById(request.getLocalId()).ifPresent(service::setBranch);
        }
        applyRequest(service, request);
        return ResponseEntity.ok(toDTO(serviceRepository.save(service)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(@AuthenticationPrincipal User user,
                                              @PathVariable Long id) {
        AutoService service = serviceRepository.findById(id)  // ← AutoService
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));

        if (!service.getBusiness().getOwner().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        service.setActive(false);
        serviceRepository.save(service);
        return ResponseEntity.noContent().build();
    }


    // PARA EL MARKETPLACE: Obtener servicios de un negocio
    @GetMapping("/public/business/{businessId}")
    public ResponseEntity<List<ServiceDTO>> getPublicServices(@PathVariable Long businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));

        List<ServiceDTO> services = serviceRepository.findByBusinessAndActiveTrue(business)
                .stream().map(this::toDTO).toList();

        return ResponseEntity.ok(services);
    }

    private void applyRequest(AutoService service, ServiceRequest r) {  // ← AutoService
        if (r.getName() != null) service.setName(r.getName());
        if (r.getDescription() != null) service.setDescription(r.getDescription());
        if (r.getCategory() != null) service.setCategory(r.getCategory());
        if (r.getPrice() != null) service.setPrice(r.getPrice());
        if (r.getDuration() != null) service.setDuration(r.getDuration());
    }

    private ServiceDTO toDTO(AutoService s) {
        ServiceDTO dto = new ServiceDTO();
        dto.setId(s.getId());
        dto.setName(s.getName());
        dto.setDescription(s.getDescription());
        dto.setCategory(s.getCategory());
        dto.setPrice(s.getPrice());
        dto.setDuration(s.getDuration());
        dto.setActive(s.getActive());
        if (s.getBranch() != null) dto.setLocalId(s.getBranch().getId());
        return dto;
    }
}
