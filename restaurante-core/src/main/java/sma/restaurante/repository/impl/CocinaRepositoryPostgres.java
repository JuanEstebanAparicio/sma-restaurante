package sma.restaurante.repository.impl;

import sma.restaurante.datasource.Jdbc;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class CocinaRepositoryPostgres {

    public record PlatoComanda(
            long idPlatillo,
            String nombrePlatillo,
            int cantidad,
            String estacion,
            int tiempoPreparacionMin
    ) {}

    public record ComandaActiva(
            long idComanda,
            long idPedido,
            String estado,
            Instant fechaFinEstimada
    ) {}

    private static final String SQL_ESTADO_PEDIDO =
            "SELECT estado FROM pedidos.pedido WHERE id_pedido = ?";

    private static final String SQL_PLATOS_PEDIDO = """
            SELECT pd.id_platillo,
                   pl.nombre,
                   pd.cantidad,
                   COALESCE(ep.estacion, 'PLANCHA') AS estacion,
                   COALESCE(ep.tiempo_preparacion_min, 12) AS tiempo_min
            FROM pedidos.pedido_detalle pd
            JOIN pedidos.platillo pl ON pl.id_platillo = pd.id_platillo
            LEFT JOIN cocina.estacion_platillo ep ON ep.id_platillo = pd.id_platillo
            WHERE pd.id_pedido = ?
            ORDER BY pd.id_detalle
            """;

    private static final String SQL_CARGA_ESTACION = """
            SELECT COUNT(*) AS activos
            FROM cocina.comanda_item
            WHERE estacion = ?
              AND estado IN ('EN_ESPERA_SINCRONIZACION', 'EN_PREPARACION')
            """;

    private static final String SQL_INSERT_COMANDA = """
            INSERT INTO cocina.comanda (
                id_pedido, prioridad, estado, tiempo_objetivo_min,
                fecha_inicio, fecha_fin_estimada
            )
            VALUES (?, ?, 'EN_PREPARACION', ?, ?, ?)
            RETURNING id_comanda
            """;

    private static final String SQL_INSERT_ITEM = """
            INSERT INTO cocina.comanda_item (
                id_comanda, id_platillo, cantidad, estacion,
                tiempo_preparacion_min, retardo_inicio_min, estado,
                fecha_inicio_programada, fecha_inicio_real, fecha_fin_estimada
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SQL_PEDIDO_EN_PREPARACION = """
            UPDATE pedidos.pedido
            SET estado = 'EN_PREPARACION'
            WHERE id_pedido = ? AND estado = 'ACEPTADO'
            """;

    private static final String SQL_ACTIVAR_ITEMS_SINCRONIZADOS = """
            UPDATE cocina.comanda_item
            SET estado = 'EN_PREPARACION',
                fecha_inicio_real = now()
            WHERE estado = 'EN_ESPERA_SINCRONIZACION'
              AND fecha_inicio_programada <= now()
            """;

    private static final String SQL_COMPLETAR_ITEMS_VENCIDOS = """
            UPDATE cocina.comanda_item
            SET estado = 'LISTO',
                fecha_fin_real = now()
            WHERE estado = 'EN_PREPARACION'
              AND fecha_fin_estimada <= now()
            """;

    private static final String SQL_COMANDAS_ACTIVAS = """
            SELECT id_comanda, id_pedido, estado, fecha_fin_estimada
            FROM cocina.comanda
            WHERE estado IN ('EN_PREPARACION', 'RETRASADA')
            """;

    private static final String SQL_ITEMS_PENDIENTES_COMANDA = """
            SELECT COUNT(*) AS pendientes
            FROM cocina.comanda_item
            WHERE id_comanda = ? AND estado <> 'LISTO'
            """;

    private static final String SQL_MARCAR_COMANDA_COMPLETADA = """
            UPDATE cocina.comanda
            SET estado = 'COMPLETADA', fecha_fin_real = now()
            WHERE id_comanda = ?
            """;

    private static final String SQL_MARCAR_PEDIDO_COMPLETADO = """
            UPDATE pedidos.pedido
            SET estado = 'COMPLETADO'
            WHERE id_pedido = ? AND estado = 'EN_PREPARACION'
            """;

    private static final String SQL_MARCAR_COMANDA_RETRASADA = """
            UPDATE cocina.comanda
            SET estado = 'RETRASADA'
            WHERE id_comanda = ? AND estado = 'EN_PREPARACION'
            """;

    public Optional<String> obtenerEstadoPedido(long idPedido) {
        return Jdbc.consultarUno(
                SQL_ESTADO_PEDIDO,
                ps -> ps.setLong(1, idPedido),
                rs -> rs.getString("estado")
        );
    }

    public List<PlatoComanda> listarPlatosPedido(long idPedido) {
        return Jdbc.consultar(
                SQL_PLATOS_PEDIDO,
                ps -> ps.setLong(1, idPedido),
                rs -> new PlatoComanda(
                        rs.getLong("id_platillo"),
                        rs.getString("nombre"),
                        rs.getInt("cantidad"),
                        rs.getString("estacion"),
                        rs.getInt("tiempo_min")
                )
        );
    }

    public int contarCargaEstacion(String estacion) {
        return Jdbc.consultarUno(
                SQL_CARGA_ESTACION,
                ps -> ps.setString(1, estacion),
                rs -> rs.getInt("activos")
        ).orElse(0);
    }

    public long registrarComanda(
            long idPedido,
            String prioridad,
            int tiempoObjetivoMin,
            Instant inicio,
            Instant finEstimada
    ) {
        return Jdbc.consultarUno(
                SQL_INSERT_COMANDA,
                ps -> {
                    ps.setLong(1, idPedido);
                    ps.setString(2, prioridad);
                    ps.setInt(3, tiempoObjetivoMin);
                    ps.setTimestamp(4, Timestamp.from(inicio));
                    ps.setTimestamp(5, Timestamp.from(finEstimada));
                },
                rs -> rs.getLong("id_comanda")
        ).orElseThrow(() -> new IllegalStateException("No se pudo crear la comanda para pedido " + idPedido));
    }

    public void registrarItemComanda(
            long idComanda,
            PlatoComanda plato,
            int retardoMin,
            String estadoInicial,
            Instant inicioProgramado,
            Instant inicioReal,
            Instant finEstimada
    ) {
        Jdbc.actualizar(SQL_INSERT_ITEM, ps -> {
            ps.setLong(1, idComanda);
            ps.setLong(2, plato.idPlatillo());
            ps.setInt(3, plato.cantidad());
            ps.setString(4, plato.estacion());
            ps.setInt(5, plato.tiempoPreparacionMin());
            ps.setInt(6, retardoMin);
            ps.setString(7, estadoInicial);
            ps.setTimestamp(8, Timestamp.from(inicioProgramado));
            ps.setTimestamp(9, inicioReal != null ? Timestamp.from(inicioReal) : null);
            ps.setTimestamp(10, Timestamp.from(finEstimada));
        });
    }

    public void marcarPedidoEnPreparacion(long idPedido) {
        Jdbc.actualizar(SQL_PEDIDO_EN_PREPARACION, ps -> ps.setLong(1, idPedido));
    }

    public int avanzarSincronizacionYCompletados() {
        int activados = Jdbc.actualizar(SQL_ACTIVAR_ITEMS_SINCRONIZADOS, Jdbc.SIN_PARAMETROS);
        int terminados = Jdbc.actualizar(SQL_COMPLETAR_ITEMS_VENCIDOS, Jdbc.SIN_PARAMETROS);
        return activados + terminados;
    }

    public List<ComandaActiva> listarComandasActivas() {
        return Jdbc.consultar(
                SQL_COMANDAS_ACTIVAS,
                Jdbc.SIN_PARAMETROS,
                rs -> new ComandaActiva(
                        rs.getLong("id_comanda"),
                        rs.getLong("id_pedido"),
                        rs.getString("estado"),
                        rs.getTimestamp("fecha_fin_estimada").toInstant()
                )
        );
    }

    public int contarItemsPendientes(long idComanda) {
        return Jdbc.consultarUno(
                SQL_ITEMS_PENDIENTES_COMANDA,
                ps -> ps.setLong(1, idComanda),
                rs -> rs.getInt("pendientes")
        ).orElse(0);
    }

    public void completarComandaYPedido(long idComanda, long idPedido) {
        Jdbc.actualizar(SQL_MARCAR_COMANDA_COMPLETADA, ps -> ps.setLong(1, idComanda));
        Jdbc.actualizar(SQL_MARCAR_PEDIDO_COMPLETADO, ps -> ps.setLong(1, idPedido));
    }

    public void marcarComandaRetrasada(long idComanda) {
        Jdbc.actualizar(SQL_MARCAR_COMANDA_RETRASADA, ps -> ps.setLong(1, idComanda));
    }
}