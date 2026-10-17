-- ============================================================================
-- Carvexio · Actualizacion de precios comerciales de los planes B2B
-- Hallazgo de auditoria: CAR-003 (todos los planes mostraban S/ 2.00 de prueba)
--
-- Precios oficiales segun la landing (frontend/app/page.tsx).
-- Ejecutar UNA sola vez contra la base de datos de PRODUCCION.
-- Idempotente: puede re-ejecutarse sin efectos secundarios (fija valores por
-- nombre de plan). Revisar el SELECT final antes de confirmar la transaccion.
-- ============================================================================

BEGIN;

UPDATE plans SET price = 0   WHERE name = 'Arranque';
UPDATE plans SET price = 149 WHERE name = 'Taller Pro';
UPDATE plans SET price = 199 WHERE name = 'Lubricentro & Grifo';
UPDATE plans SET price = 349 WHERE name = 'Multi-Sucursal';
UPDATE plans SET price = 699 WHERE name = 'Enterprise / Flotas';

-- Verificacion: revisar que cada plan tenga su precio esperado antes de COMMIT.
SELECT id, name, price FROM plans ORDER BY price;

COMMIT;
