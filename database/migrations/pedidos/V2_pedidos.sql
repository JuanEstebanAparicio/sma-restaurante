CREATE SCHEMA IF NOT EXISTS pedidos;

-- Catálogo de platillos
CREATE TABLE IF NOT EXISTS pedidos.platillo (
                                                id_platillo BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                                nombre VARCHAR(100) NOT NULL UNIQUE,
    precio NUMERIC(12,2) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'
    );

-- Receta de cada platillo conectada directamente a inventario.ingrediente
CREATE TABLE IF NOT EXISTS pedidos.platillo_ingrediente (
                                                            id_platillo BIGINT NOT NULL,
                                                            id_ingrediente BIGINT NOT NULL,
                                                            cantidad_requerida NUMERIC(12,3) NOT NULL CHECK (cantidad_requerida > 0),
    PRIMARY KEY (id_platillo, id_ingrediente),
    CONSTRAINT fk_pi_platillo
    FOREIGN KEY (id_platillo) REFERENCES pedidos.platillo(id_platillo) ON DELETE CASCADE,
    CONSTRAINT fk_pi_ingrediente
    FOREIGN KEY (id_ingrediente) REFERENCES inventario.ingrediente(id_ingrediente)
    );

-- Pedidos de las mesas (los que insertas en BD para simular clientes)
CREATE TABLE IF NOT EXISTS pedidos.pedido (
                                              id_pedido BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                              mesa VARCHAR(30) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    fecha_creacion TIMESTAMP NOT NULL DEFAULT NOW()
    );

-- Detalle de platillos por pedido
CREATE TABLE IF NOT EXISTS pedidos.pedido_platillo (
                                                       id_pedido BIGINT NOT NULL,
                                                       id_platillo BIGINT NOT NULL,
                                                       cantidad INT NOT NULL CHECK (cantidad > 0),
    PRIMARY KEY (id_pedido, id_platillo),
    CONSTRAINT fk_pp_pedido
    FOREIGN KEY (id_pedido) REFERENCES pedidos.pedido(id_pedido) ON DELETE CASCADE,
    CONSTRAINT fk_pp_platillo
    FOREIGN KEY (id_platillo) REFERENCES pedidos.platillo(id_platillo)
    );

-- ============================================================================
-- DATOS DE PRUEBA (ajustados a inventario.ingrediente e inventario.lote)
-- ============================================================================
INSERT INTO inventario.ingrediente (nombre, categoria, unidad_medida, stock_minimo)
VALUES ('Queso', 'Lacteos', 'gramos', 200.000)
    ON CONFLICT (nombre) DO NOTHING;

INSERT INTO inventario.lote (id_ingrediente, cantidad, cantidad_disponible, costo_unitario, fecha_ingreso, fecha_vencimiento)
SELECT id_ingrediente, 1000.000, 1000.000, 18.50, CURRENT_DATE, CURRENT_DATE + INTERVAL '15 days'
FROM inventario.ingrediente
WHERE nombre = 'Queso';

INSERT INTO pedidos.platillo (nombre, precio)
VALUES ('Pizza Queso', 28000.00)
    ON CONFLICT (nombre) DO NOTHING;

INSERT INTO pedidos.platillo_ingrediente (id_platillo, id_ingrediente, cantidad_requerida)
SELECT p.id_platillo, i.id_ingrediente, 150.000
FROM pedidos.platillo p, inventario.ingrediente i
WHERE p.nombre = 'Pizza Queso' AND i.nombre = 'Queso'
    ON CONFLICT DO NOTHING;

-- Pedido de prueba #101 para mesa_5
INSERT INTO pedidos.pedido (id_pedido, mesa, estado)
    OVERRIDING SYSTEM VALUE
VALUES (101, 'mesa_5', 'PENDIENTE')
    ON CONFLICT (id_pedido) DO UPDATE SET estado = 'PENDIENTE';

INSERT INTO pedidos.pedido_platillo (id_pedido, id_platillo, cantidad)
SELECT 101, id_platillo, 1
FROM pedidos.platillo
WHERE nombre = 'Pizza Queso'
    ON CONFLICT DO NOTHING;