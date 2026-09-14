package com.saas.automotriz.repository;

import com.saas.automotriz.model.Reclamacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReclamacionRepository extends JpaRepository<Reclamacion, Long> {
    Optional<Reclamacion> findByCodigoReclamo(String codigoReclamo);
}
