package com.saas.automotriz.repository;

import com.saas.automotriz.model.Business;
import com.saas.automotriz.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByBusinessAndActiveTrue(Business business);
    long countByBusinessAndActiveTrue(Business business);
    List<Product> findByBusinessAndBranchIdAndActiveTrue(Business business, Long branchId);
    List<Product> findByCategoryIgnoreCaseAndActiveTrue(String category);
    List<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name);

    List<Product> findByBrandIgnoreCaseAndActiveTrue(String brand);
    List<Product> findByPriceBetweenAndActiveTrue(Double minPrice, Double maxPrice);

    @Query("SELECT p FROM Product p WHERE p.business = :business AND p.stock <= :minStock AND p.active = true")
    List<Product> findLowStockByBusiness(@Param("business") Business business, @Param("minStock") Integer minStock);

    @Query("SELECT p FROM Product p WHERE p.business = :business AND (:branchId IS NULL OR p.branch.id = :branchId) AND p.stock <= :minStock AND p.active = true")
    List<Product> findLowStockByBusinessAndBranchId(@Param("business") Business business, @Param("branchId") Long branchId, @Param("minStock") Integer minStock);
}
