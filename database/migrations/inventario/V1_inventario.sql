CREATE SCHEMA IF NOT EXISTS inventario;

CREATE TABLE IF NOT EXISTS inventario.ingrediente (
                                                      id_ingrediente BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

                                                      nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255),

    categoria VARCHAR(50),

    unidad_medida VARCHAR(20) NOT NULL,

    stock_minimo NUMERIC(12,3) NOT NULL DEFAULT 0,

    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    observaciones VARCHAR(255),

    fecha_creacion TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_estado_ingrediente
    CHECK (
              estado IN (
              'ACTIVO',
              'INACTIVO',
              'AGOTADO',
              'VENCIDO'
                        )
    )
    );

CREATE TABLE IF NOT EXISTS inventario.lote (
                                               id_lote BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

                                               id_ingrediente BIGINT NOT NULL,

                                               cantidad NUMERIC(12,3) NOT NULL,

    cantidad_disponible NUMERIC(12,3) NOT NULL,

    costo_unitario NUMERIC(12,2) NOT NULL,

    proveedor VARCHAR(150),

    ubicacion VARCHAR(100),

    fecha_ingreso DATE NOT NULL,

    fecha_vencimiento DATE,

    CONSTRAINT fk_lote_ingrediente
    FOREIGN KEY (id_ingrediente)
    REFERENCES inventario.ingrediente(id_ingrediente),

    CONSTRAINT chk_cantidad_lote
    CHECK (
              cantidad > 0
              AND cantidad_disponible >= 0
          )
    );

CREATE TABLE IF NOT EXISTS inventario.movimiento (
                                                     id_movimiento BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

                                                     id_ingrediente BIGINT NOT NULL,

                                                     id_lote BIGINT,

                                                     tipo_movimiento VARCHAR(30) NOT NULL,

    cantidad NUMERIC(12,3) NOT NULL,

    fecha_movimiento TIMESTAMP NOT NULL DEFAULT NOW(),

    motivo VARCHAR(255),

    CONSTRAINT fk_mov_ingrediente
    FOREIGN KEY (id_ingrediente)
    REFERENCES inventario.ingrediente(id_ingrediente),

    CONSTRAINT fk_mov_lote
    FOREIGN KEY (id_lote)
    REFERENCES inventario.lote(id_lote),

    CONSTRAINT chk_tipo_movimiento
    CHECK (
              tipo_movimiento IN (
              'ENTRADA',
              'RESERVA',
              'CONSUMO',
              'MERMA',
              'AJUSTE'
                                 )
    )
    );

CREATE TABLE IF NOT EXISTS inventario.alerta (
                                                 id_alerta BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

                                                 id_ingrediente BIGINT NOT NULL,

                                                 tipo_alerta VARCHAR(50) NOT NULL,

    mensaje TEXT NOT NULL,

    activa BOOLEAN NOT NULL DEFAULT TRUE,

    fecha_generacion TIMESTAMP NOT NULL DEFAULT NOW(),

    fecha_resolucion TIMESTAMP,

    CONSTRAINT fk_alerta_ingrediente
    FOREIGN KEY (id_ingrediente)
    REFERENCES inventario.ingrediente(id_ingrediente),

    CONSTRAINT chk_tipo_alerta
    CHECK (
              tipo_alerta IN (
              'BAJO_STOCK',
              'AGOTADO',
              'POR_VENCER',
              'AGOTAMIENTO_PREDICTIVO'
                             )
    )
    );

CREATE TABLE IF NOT EXISTS inventario.prediccion_consumo (
                                                             id_prediccion BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

                                                             id_ingrediente BIGINT NOT NULL,

                                                             stock_actual NUMERIC(12,3) NOT NULL,

    consumo_promedio_diario NUMERIC(12,3) NOT NULL,

    dias_estimados_restantes NUMERIC(12,2) NOT NULL,

    fecha_calculo TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_pred_ingrediente
    FOREIGN KEY (id_ingrediente)
    REFERENCES inventario.ingrediente(id_ingrediente)
    );

CREATE INDEX idx_lote_ingrediente
    ON inventario.lote(id_ingrediente);

CREATE INDEX idx_movimiento_ingrediente
    ON inventario.movimiento(id_ingrediente);

CREATE INDEX idx_alerta_activa
    ON inventario.alerta(activa);

CREATE INDEX idx_lote_vencimiento
    ON inventario.lote(fecha_vencimiento);