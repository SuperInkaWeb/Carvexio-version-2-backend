package com.saas.automotriz.controller;

import com.saas.automotriz.dto.VehicleDTO;
import com.saas.automotriz.model.User;
import com.saas.automotriz.model.Vehicle;
import com.saas.automotriz.repository.VehicleRepository;
import com.saas.automotriz.request.VehicleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {
    private final VehicleRepository vehicleRepository;

    @GetMapping("/my")
    public List<VehicleDTO> getMyVehicles(@AuthenticationPrincipal User user) {
        return vehicleRepository.findByClientOrderByIdDesc(user).stream().map(this::toDTO).toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal User user, @RequestBody VehicleRequest request) {
        Vehicle vehicle = new Vehicle();
        vehicle.setClient(user);
        apply(vehicle, request);
        return ResponseEntity.ok(toDTO(vehicleRepository.save(vehicle)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@AuthenticationPrincipal User user, @PathVariable Long id, @RequestBody VehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findByIdAndClient(id, user).orElse(null);
        if (vehicle == null) return ResponseEntity.notFound().build();
        apply(vehicle, request);
        return ResponseEntity.ok(toDTO(vehicleRepository.save(vehicle)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        Vehicle vehicle = vehicleRepository.findByIdAndClient(id, user).orElse(null);
        if (vehicle == null) return ResponseEntity.notFound().build();
        vehicleRepository.delete(vehicle);
        return ResponseEntity.noContent().build();
    }

    private void apply(Vehicle vehicle, VehicleRequest request) {
        String type = request.getVehicleType() == null ? "" : request.getVehicleType().trim();
        String plate = request.getPlate() == null ? "" : request.getPlate().trim().toUpperCase(Locale.ROOT);
        String vin = request.getVin() == null ? "" : request.getVin().trim().toUpperCase(Locale.ROOT);

        if (type.isBlank() || type.length() > 100) throw new IllegalArgumentException("Ingresa el tipo de auto");
        if (!plate.matches("[A-Z0-9-]{5,16}")) throw new IllegalArgumentException("La placa solo admite letras, números y guiones");
        if (!vin.matches("[A-HJ-NPR-Z0-9]{17}")) throw new IllegalArgumentException("El VIN debe tener exactamente 17 caracteres alfanuméricos válidos");
        if (request.getMileage() == null || request.getMileage() < 0 || request.getMileage() > 9_999_999) throw new IllegalArgumentException("El kilometraje debe ser un número entero de hasta 7 dígitos");
        if (request.getYearsOfUse() == null || request.getYearsOfUse() < 0 || request.getYearsOfUse() > 40) throw new IllegalArgumentException("Los años de uso deben ser un número entero entre 0 y 40");

        vehicle.setVehicleType(type);
        vehicle.setPlate(plate);
        vehicle.setVin(vin);
        vehicle.setMileage(request.getMileage());
        vehicle.setYearsOfUse(request.getYearsOfUse());
    }

    private VehicleDTO toDTO(Vehicle vehicle) {
        VehicleDTO dto = new VehicleDTO();
        dto.setId(vehicle.getId());
        dto.setVehicleType(vehicle.getVehicleType());
        dto.setPlate(vehicle.getPlate());
        dto.setVin(vehicle.getVin());
        dto.setMileage(vehicle.getMileage());
        dto.setYearsOfUse(vehicle.getYearsOfUse());
        return dto;
    }
}
