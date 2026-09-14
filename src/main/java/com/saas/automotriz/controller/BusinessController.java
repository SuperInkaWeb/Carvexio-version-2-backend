package com.saas.automotriz.controller;

import com.saas.automotriz.dto.BusinessDTO;
import com.saas.automotriz.dto.BranchAvailabilityDTO;
import com.saas.automotriz.dto.BranchDTO;
import com.saas.automotriz.model.BusinessStatus;
import com.saas.automotriz.model.Branch;
import com.saas.automotriz.model.BranchAvailability;
import com.saas.automotriz.request.UpdateBusinessRequest;
import com.saas.automotriz.model.Business;
import com.saas.automotriz.model.User;
import com.saas.automotriz.repository.BusinessRepository;
import com.saas.automotriz.repository.BranchAvailabilityRepository;
import com.saas.automotriz.repository.BranchRepository;
import com.saas.automotriz.service.CloudinaryService;
import com.saas.automotriz.service.BusinessService;
import com.saas.automotriz.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/business")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessRepository businessRepository;
    private final BranchRepository branchRepository;
    private final BranchAvailabilityRepository branchAvailabilityRepository;
    private final BusinessService businessService;
    private final CloudinaryService cloudinaryService;
    private final EmailService emailService;

    @GetMapping("/me")
    public ResponseEntity<BusinessDTO> getMyBusiness(@AuthenticationPrincipal User user) {
        return businessRepository.findByOwnerId(user.getId())
                .map(business -> ResponseEntity.ok(toDTO(business)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/me")
    public ResponseEntity<BusinessDTO> createBusiness(@AuthenticationPrincipal User user,
                                                      @RequestBody UpdateBusinessRequest request) {
        Business business = new Business();
        business.setOwner(user);
        applyRequest(business, request);
        Business saved = businessRepository.save(business);
        
        try {
            emailService.sendBusinessPendingApprovalEmail(user.getEmail(), saved.getName(), user.getName());
            emailService.sendNewBusinessRegistrationNoticeToAdmin(saved.getName(), user.getEmail(), user.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(toDTO(saved));
    }

    @PutMapping("/me")
    public ResponseEntity<BusinessDTO> updateBusiness(@AuthenticationPrincipal User user,
                                                      @RequestBody UpdateBusinessRequest request) {
        return businessRepository.findByOwnerId(user.getId())
                .map(business -> {
                    applyRequest(business, request);
                    return ResponseEntity.ok(toDTO(businessRepository.save(business)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<BusinessDTO>> search(@RequestParam String query) {
        List<BusinessDTO> results = businessRepository
                .findByNameContainingIgnoreCaseOrTypeContainingIgnoreCase(query, query)
                .stream().map(this::toDTO).toList();
        return ResponseEntity.ok(results);
    }

    // Lista todos los negocios
    @GetMapping("/public")
    public ResponseEntity<List<BusinessDTO>> getAllBusinesses() {
        List<BusinessDTO> results = businessRepository.findByStatus(BusinessStatus.APPROVED)
                .stream().map(this::toDTO).toList();
        return ResponseEntity.ok(results);
    }

    // Lista negocios filtrados por tipo/categoría
    @GetMapping("/public/filter")
    public ResponseEntity<List<BusinessDTO>> getByType(@RequestParam String type) {
        List<BusinessDTO> results = businessRepository.findByTypeIgnoreCase(type)
                .stream().map(this::toDTO).toList();
        return ResponseEntity.ok(results);
    }

    @PutMapping("/me/photo")
    public ResponseEntity<BusinessDTO> uploadPhoto(@AuthenticationPrincipal User user,
                                                   @RequestParam MultipartFile file) {
        return businessRepository.findByOwnerId(user.getId())
                .map(business -> {
                    String url = cloudinaryService.uploadImage(file);
                    business.setPhotoUrl(url);
                    return ResponseEntity.ok(toDTO(businessRepository.save(business)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/me/logo")
    public ResponseEntity<BusinessDTO> uploadLogo(@AuthenticationPrincipal User user,
                                                  @RequestParam MultipartFile file) {
        return businessRepository.findByOwnerId(user.getId())
                .map(business -> {
                    String url = cloudinaryService.uploadImage(file);
                    business.setLogoUrl(url);
                    return ResponseEntity.ok(toDTO(businessRepository.save(business)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // 👁️ VER DETALLE DE UN SOLO NEGOCIO (PARA EL MARKETPLACE)
    @GetMapping("/public/{id}")
    public ResponseEntity<BusinessDTO> getBusinessPublicDetail(@PathVariable Long id) {
        return businessRepository.findById(id)
                .map(business -> ResponseEntity.ok(toDTO(business)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/me/availability")
    public ResponseEntity<List<BranchAvailabilityDTO>> getBusinessAvailability() {
        Business business = businessService.getCurrentBusiness();
        List<BranchAvailabilityDTO> result = branchAvailabilityRepository.findByBusiness(business)
                .stream()
                .map(this::toAvailabilityDTO)
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/me/availability")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<List<BranchAvailabilityDTO>> saveBusinessAvailability(@RequestBody List<BranchAvailabilityDTO> request) {
        Business business = businessService.getCurrentBusiness();
        branchAvailabilityRepository.deleteByBusiness(business);
        List<BranchAvailability> toSave = request == null ? List.of() : request.stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .map(item -> toBusinessEntity(business, item))
                .toList();
        List<BranchAvailabilityDTO> saved = branchAvailabilityRepository.saveAll(toSave)
                .stream()
                .map(this::toAvailabilityDTO)
                .toList();
        return ResponseEntity.ok(saved);
    }



    private void applyRequest(Business business, UpdateBusinessRequest r) {
        if (r.getName() != null) business.setName(r.getName());
        if (r.getDescription() != null) business.setDescription(r.getDescription());
        if (r.getDepartment() != null) business.setDepartment(r.getDepartment());
        if (r.getAddress() != null) business.setAddress(r.getAddress());
        if (r.getPhone() != null) business.setPhone(r.getPhone());
        if (r.getCategory() != null) business.setCategory(r.getCategory());
        if (r.getLatitude() != null) business.setLatitude(r.getLatitude());
        if (r.getLongitude() != null) business.setLongitude(r.getLongitude());
    }

    private BusinessDTO toDTO(Business b) {
        BusinessDTO dto = new BusinessDTO();
        dto.setId(b.getId());
        dto.setName(b.getName());
        dto.setDescription(b.getDescription());
        dto.setDepartment(b.getDepartment());
        dto.setAddress(b.getAddress());
        dto.setPhone(b.getPhone());
        dto.setCategory(b.getCategory());
        dto.setLatitude(b.getLatitude());
        dto.setLongitude(b.getLongitude());
        dto.setPhotoUrl(b.getPhotoUrl());
        dto.setLogoUrl(b.getLogoUrl());
        if (b.getStatus() != null) {
            dto.setStatus(b.getStatus().name());
        }
        if (b.getPlan() != null) {
            dto.setPlanId(b.getPlan().getId());
            dto.setPlanName(b.getPlan().getName());
            dto.setHasCrm(Boolean.TRUE.equals(b.getPlan().getHasCrm()));
        } else {
            dto.setHasCrm(false);
        }
        List<BranchDTO> branches = branchRepository.findByBusinessIdAndActiveTrue(b.getId())
                .stream()
                .map(branch -> {
                    BranchDTO branchDTO = new BranchDTO();
                    branchDTO.setId(branch.getId());
                    branchDTO.setName(branch.getName());
                    branchDTO.setAddress(branch.getAddress());
                    branchDTO.setPhone(branch.getPhone());
                    branchDTO.setDistrict(branch.getDistrict());
                    branchDTO.setLatitude(branch.getLatitude());
                    branchDTO.setLongitude(branch.getLongitude());
                    branchDTO.setActive(branch.getActive());
                    branchDTO.setBusinessId(branch.getBusiness().getId());
                    branchDTO.setCreatedAt(branch.getCreatedAt());
                    return branchDTO;
                })
                .toList();
        dto.setBranches(branches);
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

    private BranchAvailability toBusinessEntity(Business business, BranchAvailabilityDTO dto) {
        BranchAvailability availability = new BranchAvailability();
        availability.setBusiness(business);
        availability.setDayOfWeek(dto.getDayOfWeek() == null ? null : dto.getDayOfWeek().trim().toUpperCase());
        availability.setStartTime(dto.getStartTime());
        availability.setEndTime(dto.getEndTime());
        availability.setCapacity(dto.getCapacity() == null || dto.getCapacity() < 1 ? 1 : dto.getCapacity());
        return availability;
    }
}
