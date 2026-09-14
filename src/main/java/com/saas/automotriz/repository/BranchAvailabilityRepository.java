package com.saas.automotriz.repository;

import com.saas.automotriz.model.Branch;
import com.saas.automotriz.model.BranchAvailability;
import com.saas.automotriz.model.Business;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchAvailabilityRepository extends JpaRepository<BranchAvailability, Long> {
    List<BranchAvailability> findByBranch(Branch branch);
    List<BranchAvailability> findByBusiness(Business business);
    List<BranchAvailability> findByBranchAndDayOfWeek(Branch branch, String dayOfWeek);
    List<BranchAvailability> findByBusinessAndDayOfWeek(Business business, String dayOfWeek);
    void deleteByBranch(Branch branch);
    void deleteByBusiness(Business business);
}
