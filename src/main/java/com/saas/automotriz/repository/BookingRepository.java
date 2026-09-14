package com.saas.automotriz.repository;

import com.saas.automotriz.model.Booking;
import com.saas.automotriz.model.BookingStatus;
import com.saas.automotriz.model.Business;
import com.saas.automotriz.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByClient(User client);
    List<Booking> findByBusiness(Business business);
    List<Booking> findByBusinessAndDate(Business business, LocalDate date);
    List<Booking> findByBusinessAndBranchId(Business business, Long branchId);
    List<Booking> findByBusinessAndBranchIdAndDate(Business business, Long branchId, LocalDate date);
    List<Booking> findByBusinessAndBranchIdAndDateAndStatusIn(Business business, Long branchId, LocalDate date, Collection<BookingStatus> statuses);
    Long countByBusinessAndBranchIdAndDateAndTimeAndStatusIn(Business business, Long branchId, LocalDate date, LocalTime time, Collection<BookingStatus> statuses);
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.business = :business AND b.date = :date AND b.time = :time AND b.branch IS NULL AND b.status IN :statuses")
    Long countByBusinessAndDateAndTimeAndStatusInAndBranchIsNull(@Param("business") Business business,
                                                                 @Param("date") LocalDate date,
                                                                 @Param("time") LocalTime time,
                                                                 @Param("statuses") Collection<BookingStatus> statuses);

    Long countByBusinessAndDate(Business business, LocalDate date);
    Long countByBusinessAndStatus(Business business, BookingStatus status);
    Long countByBusinessAndBranchIdAndDate(Business business, Long branchId, LocalDate date);
    Long countByBusinessAndBranchIdAndStatus(Business business, Long branchId, BookingStatus status);

    @Query("SELECT b.service.name, COUNT(b) as total FROM Booking b WHERE b.business = :business GROUP BY b.service.name ORDER BY total DESC")
    List<Object[]> findTopServiceByBusiness(@Param("business") Business business);

    @Query("SELECT b.service.name, COUNT(b) as total FROM Booking b WHERE b.business = :business AND (:branchId IS NULL OR b.branch.id = :branchId) GROUP BY b.service.name ORDER BY total DESC")
    List<Object[]> findTopServiceByBusinessAndBranchId(@Param("business") Business business, @Param("branchId") Long branchId);

    @Query("SELECT COALESCE(SUM(b.service.price), 0) FROM Booking b WHERE b.business = :business AND b.status = 'COMPLETED'")
    Double sumIngresosByBusiness(@Param("business") Business business);

    @Query("SELECT COALESCE(SUM(b.service.price), 0) FROM Booking b WHERE b.business = :business AND (:branchId IS NULL OR b.branch.id = :branchId) AND b.status = 'COMPLETED'")
    Double sumIngresosByBusinessAndBranchId(@Param("business") Business business, @Param("branchId") Long branchId);

    Long countDistinctClientByBusiness(Business business);

    @Query("SELECT COUNT(DISTINCT b.client) FROM Booking b WHERE b.business = :business AND (:branchId IS NULL OR b.branch.id = :branchId)")
    Long countDistinctClientByBusinessAndBranchId(@Param("business") Business business, @Param("branchId") Long branchId);


    @Query("SELECT DISTINCT b.client FROM Booking b WHERE b.business = :business")
    List<User> findClientsByBusiness(@Param("business") Business business);

    List<Booking> findByBusinessAndClient(Business business, User client);

    @Query("SELECT b FROM Booking b WHERE b.business = :business AND b.date BETWEEN :start AND :end")
    List<Booking> findByBusinessAndDateBetween(@Param("business") Business business,
                                               @Param("start") LocalDate start,
                                               @Param("end") LocalDate end);

    @Query("SELECT b.service.name, COUNT(b) as total FROM Booking b WHERE b.business = :business AND b.status = 'COMPLETED' GROUP BY b.service.name ORDER BY total DESC")
    List<Object[]> findTopServices(@Param("business") Business business);

    @Query("SELECT b.client, COUNT(b) as total FROM Booking b WHERE b.business = :business GROUP BY b.client ORDER BY total DESC")
    List<Object[]> findTopClients(@Param("business") Business business);

    Long countBy();

    @Query("SELECT COALESCE(SUM(b.service.price), 0) FROM Booking b WHERE b.status = 'COMPLETED'")
    Double sumAllIngresos();

    List<Booking> findByStatus(BookingStatus status);
}
