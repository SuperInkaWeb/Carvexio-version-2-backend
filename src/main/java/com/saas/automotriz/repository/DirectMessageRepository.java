package com.saas.automotriz.repository;

import com.saas.automotriz.model.Business;
import com.saas.automotriz.model.DirectMessage;
import com.saas.automotriz.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {
    List<DirectMessage> findByBusinessAndClientOrderByCreatedAtAsc(Business business, User client);

    long countByClientAndSentByBusinessTrueAndReadFalse(User client);
    long countByBusinessAndSentByBusinessFalseAndReadFalse(Business business);
    long countByBusinessAndClientAndSentByBusinessTrueAndReadFalse(Business business, User client);
    long countByBusinessAndClientAndSentByBusinessFalseAndReadFalse(Business business, User client);

    @Query("SELECT DISTINCT m.client FROM DirectMessage m WHERE m.business = :business")
    List<User> findDistinctClientsByBusiness(@Param("business") Business business);
}
