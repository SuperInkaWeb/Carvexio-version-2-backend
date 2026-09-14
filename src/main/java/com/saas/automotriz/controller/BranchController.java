package com.saas.automotriz.controller;

import com.saas.automotriz.dto.BranchAvailabilityDTO;
import com.saas.automotriz.dto.BranchDTO;
import com.saas.automotriz.model.Branch;
import com.saas.automotriz.model.BranchAvailability;
import com.saas.automotriz.model.Business;
import com.saas.automotriz.repository.BranchRepository;
import com.saas.automotriz.repository.BranchAvailabilityRepository;
import com.saas.automotriz.request.BranchRequest;
import com.saas.automotriz.service.BusinessService;
import com.saas.automotriz.service.PlanLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.*;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchRepository branchRepository;
    private final BranchAvailabilityRepository branchAvailabilityRepository;
    private final BusinessService businessService;
    private final PlanLimitService planLimitService;

    // GET /api/branches/me - Listar todos los locales activos del negocio autenticado
    @GetMapping("/me")
    public ResponseEntity<List<BranchDTO>> getMyBranches() {
        Business business = businessService.getCurrentBusiness();
        List<BranchDTO> branches = branchRepository.findByBusinessIdAndActiveTrue(business.getId())
                .stream().map(this::toDTO).toList();
        return ResponseEntity.ok(branches);
    }

    // POST /api/branches/me - Crear un nuevo local para el negocio autenticado
    @PostMapping("/me")
    public ResponseEntity<BranchDTO> createBranch(@RequestBody BranchRequest request) {
        Business business = businessService.getCurrentBusiness();
        planLimitService.checkBranchLimit(business, branchRepository.countByBusinessIdAndActiveTrue(business.getId()));

        Branch branch = new Branch();
        branch.setBusiness(business);
        applyRequest(branch, request);

        return ResponseEntity.ok(toDTO(branchRepository.save(branch)));
    }

    // PUT /api/branches/me/{id} - Editar un local (verificando que pertenezca al negocio)
    @PutMapping("/me/{id}")
    public ResponseEntity<BranchDTO> updateBranch(@PathVariable Long id, @RequestBody BranchRequest request) {
        Business business = businessService.getCurrentBusiness();

        return branchRepository.findById(id)
                .filter(b -> b.getBusiness().getId().equals(business.getId()))
                .map(branch -> {
                    applyRequest(branch, request);
                    return ResponseEntity.ok(toDTO(branchRepository.save(branch)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/branches/me/{id} - Eliminar lógicamente un local (soft-delete)
    @DeleteMapping("/me/{id}")
    public ResponseEntity<Void> deleteBranch(@PathVariable Long id) {
        Business business = businessService.getCurrentBusiness();

        return branchRepository.findById(id)
                .filter(b -> b.getBusiness().getId().equals(business.getId()))
                .map(branch -> {
                    branch.setActive(false);
                    branchRepository.save(branch);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/me/{id}/availability")
    public ResponseEntity<?> getBranchAvailability(@PathVariable Long id) {
        Business business = businessService.getCurrentBusiness();

        return branchRepository.findById(id)
                .filter(b -> b.getBusiness().getId().equals(business.getId()))
                .map(branch -> ResponseEntity.ok(
                        branchAvailabilityRepository.findByBranch(branch)
                                .stream()
                                .map(this::toAvailabilityDTO)
                                .sorted(Comparator.comparing(dto -> DayOfWeek.valueOf(dto.getDayOfWeek())))
                                .toList()
                ))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/me/{id}/availability")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> saveBranchAvailability(
            @PathVariable Long id,
            @RequestBody List<BranchAvailabilityDTO> request
    ) {
        Business business = businessService.getCurrentBusiness();

        return branchRepository.findById(id)
                .filter(b -> b.getBusiness().getId().equals(business.getId()))
                .map(branch -> {
                    branchAvailabilityRepository.deleteByBranch(branch);
                    branchAvailabilityRepository.flush();

                    Map<String, BranchAvailabilityDTO> uniqueItemsMap = new LinkedHashMap<>();
                    if (request != null) {
                        for (BranchAvailabilityDTO item : request) {
                            if (Boolean.TRUE.equals(item.getEnabled()) && item.getDayOfWeek() != null) {
                                uniqueItemsMap.put(item.getDayOfWeek().toUpperCase(), item);
                            }
                        }
                    }

                    List<BranchAvailability> toSave = uniqueItemsMap.values().stream()
                            .map(item -> toEntity(branch, item))
                            .toList();

                    List<BranchAvailabilityDTO> saved = branchAvailabilityRepository.saveAll(toSave)
                            .stream()
                            .map(this::toAvailabilityDTO)
                            .sorted(Comparator.comparing(dto -> DayOfWeek.valueOf(dto.getDayOfWeek())))
                            .toList();

                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Helpers privados ---

    private void applyRequest(Branch branch, BranchRequest r) {
        if (r.getName() != null) branch.setName(r.getName());
        if (r.getAddress() != null) branch.setAddress(r.getAddress());
        if (r.getPhone() != null) branch.setPhone(r.getPhone());
        if (r.getDistrict() != null) branch.setDistrict(r.getDistrict());
        if (r.getLatitude() != null) branch.setLatitude(r.getLatitude());
        if (r.getLongitude() != null) branch.setLongitude(r.getLongitude());
    }

    private BranchDTO toDTO(Branch b) {
        BranchDTO dto = new BranchDTO();
        dto.setId(b.getId());
        dto.setName(b.getName());
        dto.setAddress(b.getAddress());
        dto.setPhone(b.getPhone());
        dto.setDistrict(b.getDistrict());
        dto.setLatitude(b.getLatitude());
        dto.setLongitude(b.getLongitude());
        dto.setActive(b.getActive());
        dto.setBusinessId(b.getBusiness().getId());
        dto.setCreatedAt(b.getCreatedAt());
        return dto;
    }

    private BranchAvailabilityDTO toAvailabilityDTO(BranchAvailability availability) {
        BranchAvailabilityDTO dto = new BranchAvailabilityDTO();
        dto.setId(availability.getId());
        dto.setBranchId(availability.getBranch() != null ? availability.getBranch().getId() : null);
        dto.setBusinessId(availability.getBusiness() != null ? availability.getBusiness().getId() : null);
        dto.setDayOfWeek(availability.getDayOfWeek());
        dto.setStartTime(availability.getStartTime());
        dto.setEndTime(availability.getEndTime());
        dto.setCapacity(availability.getCapacity());
        dto.setEnabled(true);
        return dto;
    }

    private BranchAvailability toEntity(Branch branch, BranchAvailabilityDTO dto) {
        BranchAvailability availability = new BranchAvailability();
        availability.setBranch(branch);
        availability.setBusiness(branch.getBusiness());
        availability.setDayOfWeek(normalizeDay(dto.getDayOfWeek()));
        availability.setStartTime(dto.getStartTime());
        availability.setEndTime(dto.getEndTime());
        availability.setCapacity(dto.getCapacity() == null || dto.getCapacity() < 1 ? 1 : dto.getCapacity());
        return availability;
    }

    private String normalizeDay(String dayOfWeek) {
        return dayOfWeek == null ? null : dayOfWeek.trim().toUpperCase(Locale.ROOT);
    }
}
