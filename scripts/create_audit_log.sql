-- ============================================================================
-- Carvexio · Tabla de trazabilidad de eventos de seguridad (AuditLog)
-- El proyecto no usa migraciones automaticas (ddl-auto no crea tablas en prod),
-- por lo que esta tabla debe crearse manualmente una vez en cada entorno.
-- AuditService tolera su ausencia (degrada sin romper), pero sin la tabla no se
-- registra nada.
-- ============================================================================

CREATE TABLE IF NOT EXISTS audit_logs (
    id           BIGSERIAL PRIMARY KEY,
    actor_email  VARCHAR(255),
    actor_role   VARCHAR(50),
    action       VARCHAR(100),
    target_type  VARCHAR(50),
    target_id    BIGINT,
    details      VARCHAR(1000),
    created_at   TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs (action);
