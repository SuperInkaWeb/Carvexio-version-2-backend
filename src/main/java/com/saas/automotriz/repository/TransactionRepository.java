package com.saas.automotriz.repository;

import com.saas.automotriz.model.Transaction;
import com.saas.automotriz.model.TransactionType;
import com.saas.automotriz.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByClient(User client);
    List<Transaction> findByClientAndType(User client, TransactionType type);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.client = :client AND t.status = 'COMPLETED'")
    Double sumCompletedAmountByClient(@Param("client") User client);
}