package com.saas.automotriz.service;

import com.saas.automotriz.model.AuditLog;
import com.saas.automotriz.model.User;
import com.saas.automotriz.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Registro de eventos de seguridad (trazabilidad). La escritura es tolerante a
 * fallos: si la tabla no existe todavia o falla el insert, se loguea un warning
 * pero NUNCA se interrumpe la operacion principal.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(User actor, String action, String targetType, Long targetId, String details) {
        try {
            AuditLog entry = new AuditLog();
            if (actor != null) {
                entry.setActorEmail(actor.getEmail());
                entry.setActorRole(actor.getRole() != null ? actor.getRole().name() : null);
            }
            entry.setAction(action);
            entry.setTargetType(targetType);
            entry.setTargetId(targetId);
            entry.setDetails(details);
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("No se pudo registrar el evento de auditoria ({} {} {})", action, targetType, targetId, e);
        }
    }
}
