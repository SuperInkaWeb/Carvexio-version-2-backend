-- ============================================================================
-- Carvexio · Documentos KYC privados (ID-004b)
-- Columnas necesarias para re-firmar la URL de entrega de los documentos
-- subidos a Cloudinary como 'authenticated'. ddl-auto no altera tablas en prod,
-- por lo que esta migracion debe correrse manualmente una vez por entorno.
-- Los documentos ya existentes quedan con public_id NULL: el backend sigue
-- devolviendo su URL almacenada (legacy, publica) hasta que se vuelvan a subir.
-- ============================================================================

ALTER TABLE business_documents ADD COLUMN IF NOT EXISTS public_id     VARCHAR(255);
ALTER TABLE business_documents ADD COLUMN IF NOT EXISTS resource_type VARCHAR(20);
