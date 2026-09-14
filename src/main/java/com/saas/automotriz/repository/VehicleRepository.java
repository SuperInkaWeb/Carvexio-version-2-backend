package com.saas.automotriz.repository;

import com.saas.automotriz.model.User;
import com.saas.automotriz.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByClientOrderByIdDesc(User client);
    Optional<Vehicle> findByIdAndClient(Long id, User client);
}
