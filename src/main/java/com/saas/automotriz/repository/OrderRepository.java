package com.saas.automotriz.repository;

import com.saas.automotriz.model.Business;
import com.saas.automotriz.model.Order;
import com.saas.automotriz.model.OrderStatus;
import com.saas.automotriz.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByClientOrderByCreatedAtDesc(User client);
    List<Order> findByBusinessOrderByCreatedAtDesc(Business business);

    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i WHERE o.business = :business AND (:branchId IS NULL OR i.product.branch.id = :branchId) ORDER BY o.createdAt DESC")
    List<Order> findByBusinessAndBranchIdOrderByCreatedAtDesc(@Param("business") Business business, @Param("branchId") Long branchId);

    Long countByBusiness(Business business);
    Long countByBusinessAndStatus(Business business, OrderStatus status);

    @Query("SELECT COUNT(DISTINCT o) FROM Order o JOIN o.items i WHERE o.business = :business AND (:branchId IS NULL OR i.product.branch.id = :branchId)")
    Long countByBusinessAndBranchId(@Param("business") Business business, @Param("branchId") Long branchId);

    @Query("SELECT COUNT(DISTINCT o) FROM Order o JOIN o.items i WHERE o.business = :business AND o.status = :status AND (:branchId IS NULL OR i.product.branch.id = :branchId)")
    Long countByBusinessAndStatusAndBranchId(@Param("business") Business business, @Param("status") OrderStatus status, @Param("branchId") Long branchId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0.0) FROM Order o WHERE o.business = :business AND o.status != 'CANCELLED' AND o.status != 'PENDING'")
    Double sumIngresosByBusiness(@Param("business") Business business);

    @Query("SELECT COALESCE(SUM(i.subtotal), 0.0) FROM OrderItem i WHERE i.order.business = :business AND i.order.status != 'CANCELLED' AND i.order.status != 'PENDING' AND (:branchId IS NULL OR i.product.branch.id = :branchId)")
    Double sumIngresosByBusinessAndBranchId(@Param("business") Business business, @Param("branchId") Long branchId);

    @Query("SELECT DISTINCT o.client FROM Order o WHERE o.business = :business")
    List<User> findClientsByBusiness(@Param("business") Business business);

    @Query("SELECT DISTINCT o.client FROM Order o JOIN o.items i WHERE o.business = :business AND (:branchId IS NULL OR i.product.branch.id = :branchId)")
    List<User> findClientsByBusinessAndBranchId(@Param("business") Business business, @Param("branchId") Long branchId);

    List<Order> findByBusinessAndClient(Business business, User client);

    @Query("SELECT i.product.name, SUM(i.quantity) as total FROM OrderItem i WHERE i.order.business = :business AND i.order.status != 'CANCELLED' AND i.order.status != 'PENDING' GROUP BY i.product.name ORDER BY total DESC")
    List<Object[]> findTopProductByBusiness(@Param("business") Business business);

    @Query("SELECT i.product.name, SUM(i.quantity) as total FROM OrderItem i WHERE i.order.business = :business AND i.order.status != 'CANCELLED' AND i.order.status != 'PENDING' AND (:branchId IS NULL OR i.product.branch.id = :branchId) GROUP BY i.product.name ORDER BY total DESC")
    List<Object[]> findTopProductByBusinessAndBranchId(@Param("business") Business business, @Param("branchId") Long branchId);
}
