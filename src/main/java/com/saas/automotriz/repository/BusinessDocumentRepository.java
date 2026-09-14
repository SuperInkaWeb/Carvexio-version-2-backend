package com.saas.automotriz.repository;

import com.saas.automotriz.model.BusinessDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessDocumentRepository extends JpaRepository<BusinessDocument, Long> {

    List<BusinessDocument> findByBusinessIdOrderByCreatedAtDesc(Long businessId);
}
