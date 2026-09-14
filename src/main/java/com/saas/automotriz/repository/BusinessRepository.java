package com.saas.automotriz.repository;

import com.saas.automotriz.model.Business;
import com.saas.automotriz.model.BusinessStatus;
import com.saas.automotriz.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusinessRepository extends JpaRepository<Business, Long> {
    Optional<Business> findByOwnerId(Long ownerId);

    // búsqueda por nombre o tipo
    List<Business> findByNameContainingIgnoreCaseOrTypeContainingIgnoreCase(String name, String type);

    List<Business> findByTypeIgnoreCase(String type);

    Long countBy();

    List<Business> findByStatus(BusinessStatus status);
    List<Business> findByStatusAndNameContainingIgnoreCaseOrStatusAndCategoryContainingIgnoreCase(
            BusinessStatus s1, String name, BusinessStatus s2, String category);
}

