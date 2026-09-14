package com.saas.automotriz.repository;

import com.saas.automotriz.model.AutoService;
import com.saas.automotriz.model.Business;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository extends JpaRepository<AutoService, Long> {
    List<AutoService> findByBusinessAndActiveTrue(Business business);
    long countByBusinessAndActiveTrue(Business business);
    List<AutoService> findByBusinessAndBranchIdAndActiveTrue(Business business, Long branchId);
}