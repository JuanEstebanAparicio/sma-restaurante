-- Limpia alertas activas duplicadas (conserva la mas antigua de cada par)
UPDATE inventario.alerta a
SET activa = FALSE, fecha_resolucion = NOW()
WHERE a.activa
  AND EXISTS (
    SELECT 1 FROM inventario.alerta b
    WHERE b.activa
      AND b.id_ingrediente = a.id_ingrediente
      AND b.tipo_alerta = a.tipo_alerta
      AND b.id_alerta < a.id_alerta
);

-- Una sola alerta activa por ingrediente y tipo
CREATE UNIQUE INDEX IF NOT EXISTS uq_alerta_activa
    ON inventario.alerta (id_ingrediente, tipo_alerta)
    WHERE activa;

-- Nuevo tipo: lote vencido con existencias
ALTER TABLE inventario.alerta DROP CONSTRAINT IF EXISTS chk_tipo_alerta;
ALTER TABLE inventario.alerta ADD CONSTRAINT chk_tipo_alerta
    CHECK (tipo_alerta IN ('BAJO_STOCK','AGOTADO','POR_VENCER','VENCIDO','AGOTAMIENTO_PREDICTIVO'));

-- Un lote no puede tener mas disponible que lo ingresado
ALTER TABLE inventario.lote DROP CONSTRAINT IF EXISTS chk_disponible_maximo;
ALTER TABLE inventario.lote ADD CONSTRAINT chk_disponible_maximo
    CHECK (cantidad_disponible <= cantidad);

CREATE INDEX IF NOT EXISTS idx_movimiento_fecha
    ON inventario.movimiento (fecha_movimiento);