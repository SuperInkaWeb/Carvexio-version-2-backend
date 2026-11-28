package com.saas.automotriz.repository;

import com.saas.automotriz.model.Branch;
import com.saas.automotriz.model.BranchAvailability;
import com.saas.automotriz.model.Business;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BranchAvailabilityRepository extends JpaRepository<BranchAvailability, Long> {
    List<BranchAvailability> findByBranch(Branch branch);
    List<BranchAvailability> findByBusiness(Business business);
    List<BranchAvailability> findByBranchAndDayOfWeek(Branch branch, String dayOfWeek);
    List<BranchAvailability> findByBusinessAndDayOfWeek(Business business, String dayOfWeek);
    void deleteByBranch(Branch branch);
    void deleteByBusiness(Business business);

    // Variantes con bloqueo pesimista: serializan la verificacion+reserva de cupos
    // de un mismo (sucursal/negocio, dia) para evitar la sobre-reserva por condicion
    // de carrera (ID-005). Usar solo dentro de la transaccion de createBooking.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM BranchAvailability a WHERE a.branch = :branch AND a.dayOfWeek = :dayOfWeek")
    List<BranchAvailability> lockByBranchAndDayOfWeek(@Param("branch") Branch branch, @Param("dayOfWeek") String dayOfWeek);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM BranchAvailability a WHERE a.business = :business AND a.dayOfWeek = :dayOfWeek")
    List<BranchAvailability> lockByBusinessAndDayOfWeek(@Param("business") Business business, @Param("dayOfWeek") String dayOfWeek);
}
