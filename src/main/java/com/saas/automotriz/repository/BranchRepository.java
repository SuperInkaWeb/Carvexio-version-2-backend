package com.saas.automotriz.repository;

import com.saas.automotriz.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByBusinessIdAndActiveTrue(Long businessId);
    long countByBusinessIdAndActiveTrue(Long businessId);
}
