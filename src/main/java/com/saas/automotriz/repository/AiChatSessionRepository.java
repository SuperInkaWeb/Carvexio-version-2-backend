package com.saas.automotriz.repository;

import com.saas.automotriz.model.AiChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiChatSessionRepository extends JpaRepository<AiChatSession, Long> {

    Optional<AiChatSession> findByPublicId(UUID publicId);

    @Query("SELECT s FROM AiChatSession s LEFT JOIN FETCH s.messages WHERE s.user.id = :userId ORDER BY s.createdAt DESC")
    List<AiChatSession> findSessionsWithMessagesByUserId(@Param("userId") Long userId);
}
