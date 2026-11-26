package pedidos.DataAccessObject;

import pedidos.Config.ConexionBD;
import pedidos.model.IngredienteRequerido;
import pedidos.model.PedidoPendiente;
import pedidos.model.PlatilloDetalle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PedidoDAO {

    public static class OpcionMenu {
        public final long idPlatillo;
        public final String nombre;
        public final double precio;
        public final int porcionesDisponibles;

        public OpcionMenu(long idPlatillo, String nombre, double precio, int porcionesDisponibles) {
            this.idPlatillo = idPlatillo;
            this.nombre = nombre;
            this.precio = precio;
            this.porcionesDisponibles = porcionesDisponibles;
        }
    }

    public static class DemandaSemanalPlatillo {
        public final String nombrePlatillo;
        public final int porcionesPedidasSemana;

        public DemandaSemanalPlatillo(String nombrePlatillo, int porcionesPedidasSemana) {
            this.nombrePlatillo = nombrePlatillo;
            this.porcionesPedidasSemana = porcionesPedidasSemana;
        }
    }

    public static class ItemStockPredictivo {
        public final String nombre;
        public final String unidadMedida;
        public final double stockDisponible;
        public final double stockMinimo;
        public final double consumoComprometido;
        public final int platosSemanaAsociados;
        public final String detallePlatosSemana;
        public final double tasaConsumoDiario;
        public final double gastoPredictivo7Dias;
        public final double diasAutonomia;
        public final double reposicionSugerida;
        public final String diagnostico;

        public ItemStockPredictivo(String nombre, String unidadMedida, double stockDisponible,
                                   double stockMinimo, double consumoComprometido,
                                   double gastoSemanalPorPlatos, int platosSemanaAsociados,
                                   String detallePlatosSemana) {
            this.nombre = nombre;
            this.unidadMedida = unidadMedida;
            this.stockDisponible = stockDisponible;
            this.stockMinimo = stockMinimo;
            this.consumoComprometido = consumoComprometido;
            this.platosSemanaAsociados = platosSemanaAsociados;
            this.detallePlatosSemana = (detallePlatosSemana != null && !detallePlatosSemana.isBlank())
                    ? detallePlatosSemana
                    : "Sin pedidos en los últimos 7 días";

            // Gasto predictivo dinámico a 7 días basado en los platos ordenados en la semana
            this.gastoPredictivo7Dias = gastoSemanalPorPlatos;
            this.tasaConsumoDiario = gastoSemanalPorPlatos / 7.0;

            double stockNeto = Math.max(0.0, stockDisponible - consumoComprometido);
            this.diasAutonomia = (this.tasaConsumoDiario > 0)
                    ? (stockNeto / this.tasaConsumoDiario)
                    : -1.0;

            // Cobertura objetivo = demanda semanal proyectada por platos + stock mínimo de seguridad
            double coberturaObjetivo = this.gastoPredictivo7Dias + stockMinimo;
            this.reposicionSugerida = Math.max(0.0, coberturaObjetivo - stockNeto);

            if (stockDisponible <= 0) {
                this.diagnostico = "AGOTADO";
            } else if (this.diasAutonomia >= 0 && this.diasAutonomia < 2.0) {
                this.diagnostico = "QUIEBRE INMINENTE";
            } else if ((this.diasAutonomia >= 0 && this.diasAutonomia < 7.0) || this.reposicionSugerida > 0) {
                this.diagnostico = "REABASTECER (7D)";
            } else if (stockDisponible < stockMinimo) {
                this.diagnostico = "BAJO MÍNIMO";
            } else {
                this.diagnostico = "ÓPTIMO";
            }
        }
    }

    public List<OpcionMenu> listarMenuConDisponibilidad() {
        List<OpcionMenu> menu = new ArrayList<>();
        String sql = """
                SELECT pl.id_platillo,
                       pl.nombre,
                       pl.precio,
                       COALESCE(MIN(FLOOR(COALESCE(st.disponible, 0) / NULLIF(pi.cantidad, 0))), 0)::int AS porciones
                FROM pedidos.platillo pl
                LEFT JOIN pedidos.platillo_ingrediente pi ON pi.id_platillo = pl.id_platillo
                LEFT JOIN (
                    SELECT id_ingrediente, SUM(cantidad_disponible) AS disponible
                    FROM inventario.lote
                    WHERE fecha_vencimiento IS NULL OR fecha_vencimiento >= CURRENT_DATE
                    GROUP BY id_ingrediente
                ) st ON st.id_ingrediente = pi.id_ingrediente
                WHERE pl.estado = 'ACTIVO'
                GROUP BY pl.id_platillo, pl.nombre, pl.precio
                ORDER BY pl.id_platillo ASC
                """;

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                menu.add(new OpcionMenu(
                        rs.getLong("id_platillo"),
                        rs.getString("nombre"),
                        rs.getDouble("precio"),
                        rs.getInt("porciones")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[PedidoDAO] Error cargando menú: " + e.getMessage());
        }
        return menu;
    }

    public long crearPedidoDesdeConsola(String mesa, Map<Long, Integer> platillosYCantidades) {
        String sqlInsertPedido = """
                INSERT INTO pedidos.pedido (estado, total, observaciones)
                VALUES ('RECIBIDO', 0, ?)
                RETURNING id_pedido
                """;

        String sqlPrecioPlatillo = "SELECT precio FROM pedidos.platillo WHERE id_platillo = ?";

        String sqlInsertDetalle = """
                INSERT INTO pedidos.pedido_detalle (id_pedido, id_platillo, cantidad, precio_unitario)
                VALUES (?, ?, ?, ?)
                """;

        String sqlActualizarTotal = """
                UPDATE pedidos.pedido
                SET total = COALESCE((
                    SELECT SUM(cantidad * precio_unitario)
                    FROM pedidos.pedido_detalle
                    WHERE id_pedido = ?
                ), 0)
                WHERE id_pedido = ?
                """;

        String sqlSnapshot = """
                INSERT INTO pedidos.pedido_ingrediente (id_pedido, id_ingrediente, cantidad_requerida, unidad_medida)
                SELECT pd.id_pedido, pi.id_ingrediente, SUM(pd.cantidad * pi.cantidad), i.unidad_medida
                FROM pedidos.pedido_detalle pd
                JOIN pedidos.platillo_ingrediente pi ON pi.id_platillo = pd.id_platillo
                JOIN inventario.ingrediente i ON i.id_ingrediente = pi.id_ingrediente
                WHERE pd.id_pedido = ?
                GROUP BY pd.id_pedido, pi.id_ingrediente, i.unidad_medida
                ON CONFLICT (id_pedido, id_ingrediente)
                DO UPDATE SET cantidad_requerida = EXCLUDED.cantidad_requerida
                """;

        try (Connection conn = ConexionBD.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                long idPedido;
                try (PreparedStatement psPed = conn.prepareStatement(sqlInsertPedido)) {
                    psPed.setString(1, mesa);
                    try (ResultSet rs = psPed.executeQuery()) {
                        rs.next();
                        idPedido = rs.getLong("id_pedido");
                    }
                }

                try (PreparedStatement psPrecio = conn.prepareStatement(sqlPrecioPlatillo);
                     PreparedStatement psDet = conn.prepareStatement(sqlInsertDetalle)) {
                    for (Map.Entry<Long, Integer> item : platillosYCantidades.entrySet()) {
                        psPrecio.setLong(1, item.getKey());
                        double precio = 0;
                        try (ResultSet rsP = psPrecio.executeQuery()) {
                            if (rsP.next()) {
                                precio = rsP.getDouble("precio");
                            }
                        }
                        psDet.setLong(1, idPedido);
                        psDet.setLong(2, item.getKey());
                        psDet.setInt(3, item.getValue());
                        psDet.setDouble(4, precio);
                        psDet.addBatch();
                    }
                    psDet.executeBatch();
                }

                try (PreparedStatement psTot = conn.prepareStatement(sqlActualizarTotal)) {
                    psTot.setLong(1, idPedido);
                    psTot.setLong(2, idPedido);
                    psTot.executeUpdate();
                }

                try (PreparedStatement psSnap = conn.prepareStatement(sqlSnapshot)) {
                    psSnap.setLong(1, idPedido);
                    psSnap.executeUpdate();
                }

                conn.commit();
                return idPedido;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("[PedidoDAO] Error creando pedido desde consola: " + e.getMessage());
            return -1L;
        }
    }

    public String obtenerEstadoPedido(long idPedido) {
        String sql = "SELECT estado FROM pedidos.pedido WHERE id_pedido = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("estado");
                }
            }
        } catch (SQLException ignored) {
        }
        return "DESCONOCIDO";
    }

    public List<PedidoPendiente> obtenerPedidosRecibidos() {
        List<PedidoPendiente> pedidos = new ArrayList<>();

        String sqlPedidos = """
                SELECT id_pedido, observaciones
                FROM pedidos.pedido
                WHERE estado = 'RECIBIDO'
                ORDER BY id_pedido ASC
                """;

        String sqlDetalles = """
                SELECT p.id_platillo, p.nombre, pd.cantidad, pd.precio_unitario
                FROM pedidos.pedido_detalle pd
                JOIN pedidos.platillo p ON pd.id_platillo = p.id_platillo
                WHERE pd.id_pedido = ?
                """;

        String sqlRecetaIngredientes = """
                SELECT i.id_ingrediente,
                       i.nombre AS ingrediente_nombre,
                       i.unidad_medida,
                       SUM(pi.cantidad * pd.cantidad) AS total_requerido
                FROM pedidos.pedido_detalle pd
                JOIN pedidos.platillo_ingrediente pi ON pd.id_platillo = pi.id_platillo
                JOIN inventario.ingrediente i ON pi.id_ingrediente = i.id_ingrediente
                WHERE pd.id_pedido = ?
                GROUP BY i.id_ingrediente, i.nombre, i.unidad_medida
                ORDER BY i.id_ingrediente ASC
                """;

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement psPedidos = conn.prepareStatement(sqlPedidos);
             ResultSet rsPedidos = psPedidos.executeQuery()) {

            while (rsPedidos.next()) {
                long idPedido = rsPedidos.getLong("id_pedido");
                String observaciones = rsPedidos.getString("observaciones");
                PedidoPendiente pedido = new PedidoPendiente(idPedido, observaciones);

                try (PreparedStatement psDet = conn.prepareStatement(sqlDetalles)) {
                    psDet.setLong(1, idPedido);
                    try (ResultSet rsDet = psDet.executeQuery()) {
                        while (rsDet.next()) {
                            pedido.agregarPlatillo(new PlatilloDetalle(
                                    rsDet.getLong("id_platillo"),
                                    rsDet.getString("nombre"),
                                    rsDet.getInt("cantidad"),
                                    rsDet.getDouble("precio_unitario")
                            ));
                        }
                    }
                }

                try (PreparedStatement psIng = conn.prepareStatement(sqlRecetaIngredientes)) {
                    psIng.setLong(1, idPedido);
                    try (ResultSet rsIng = psIng.executeQuery()) {
                        while (rsIng.next()) {
                            pedido.agregarIngrediente(
                                    rsIng.getLong("id_ingrediente"),
                                    rsIng.getString("ingrediente_nombre"),
                                    rsIng.getString("unidad_medida"),
                                    rsIng.getDouble("total_requerido")
                            );
                        }
                    }
                }

                if (!pedido.getPlatillos().isEmpty() && !pedido.getIngredientes().isEmpty()) {
                    pedidos.add(pedido);
                }
            }

        } catch (SQLException e) {
            System.err.println("[PedidoDAO] Error consultando pedidos recibidos: " + e.getMessage());
        }

        return pedidos;
    }

    public void registrarSnapshotYValidar(PedidoPendiente pedido) {
        String sqlInsertSnapshot = """
                INSERT INTO pedidos.pedido_ingrediente (id_pedido, id_ingrediente, cantidad_requerida, unidad_medida)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (id_pedido, id_ingrediente)
                DO UPDATE SET cantidad_requerida = EXCLUDED.cantidad_requerida,
                              unidad_medida = EXCLUDED.unidad_medida
                """;

        String sqlUpdatePedido = """
                UPDATE pedidos.pedido
                SET estado = 'VALIDANDO_INVENTARIO',
                    total = ?
                WHERE id_pedido = ?
                """;

        try (Connection conn = ConexionBD.obtenerConexion()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psSnap = conn.prepareStatement(sqlInsertSnapshot);
                 PreparedStatement psPed = conn.prepareStatement(sqlUpdatePedido)) {

                for (IngredienteRequerido ing : pedido.getIngredientes().values()) {
                    psSnap.setLong(1, pedido.getIdPedido());
                    psSnap.setLong(2, ing.getIdIngrediente());
                    psSnap.setDouble(3, ing.getCantidadRequerida());
                    psSnap.setString(4, ing.getUnidadMedida());
                    psSnap.addBatch();
                }
                psSnap.executeBatch();

                psPed.setDouble(1, pedido.calcularTotal());
                psPed.setLong(2, pedido.getIdPedido());
                psPed.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("[PedidoDAO] Error guardando snapshot del pedido #" + pedido.getIdPedido() + ": " + e.getMessage());
        }
    }

    public void actualizarEstado(long idPedido, String nuevoEstado) {
        String sql = "UPDATE pedidos.pedido SET estado = ? WHERE id_pedido = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setLong(2, idPedido);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[PedidoDAO] Error actualizando estado del pedido #" + idPedido + ": " + e.getMessage());
        }
    }

    public void confirmarDescuentoInventarioFinal(PedidoPendiente pedido) {
        String sqlReservasActivas = """
                SELECT r.id_reserva, r.id_ingrediente, r.id_lote, r.cantidad_reservada,
                       i.nombre, i.unidad_medida
                FROM inventario.reserva_ingrediente r
                JOIN inventario.ingrediente i ON i.id_ingrediente = r.id_ingrediente
                WHERE r.id_pedido = ? AND r.estado = 'ACTIVA'
                """;

        String sqlMarcarConsumida = """
                UPDATE inventario.reserva_ingrediente
                SET estado = 'CONSUMIDA'
                WHERE id_reserva = ?
                """;

        String sqlInsertMovimiento = """
                INSERT INTO inventario.movimiento (id_ingrediente, id_lote, tipo_movimiento, cantidad, motivo)
                VALUES (?, ?, 'CONSUMO', ?, ?)
                """;

        String sqlStockActual = """
                SELECT COALESCE(SUM(cantidad_disponible), 0) AS restante
                FROM inventario.lote
                WHERE id_ingrediente = ?
                  AND (fecha_vencimiento IS NULL OR fecha_vencimiento >= CURRENT_DATE)
                """;

        try (Connection conn = ConexionBD.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                List<String> resumenDescuentos = new ArrayList<>();
                boolean huboReservas = false;

                try (PreparedStatement psRes = conn.prepareStatement(sqlReservasActivas);
                     PreparedStatement psUpdRes = conn.prepareStatement(sqlMarcarConsumida);
                     PreparedStatement psMov = conn.prepareStatement(sqlInsertMovimiento);
                     PreparedStatement psStock = conn.prepareStatement(sqlStockActual)) {

                    psRes.setLong(1, pedido.getIdPedido());
                    try (ResultSet rs = psRes.executeQuery()) {
                        while (rs.next()) {
                            huboReservas = true;
                            long idReserva = rs.getLong("id_reserva");
                            long idIng = rs.getLong("id_ingrediente");
                            long idLote = rs.getLong("id_lote");
                            double cant = rs.getDouble("cantidad_reservada");
                            String nombre = rs.getString("nombre");
                            String unidad = rs.getString("unidad_medida");

                            psUpdRes.setLong(1, idReserva);
                            psUpdRes.executeUpdate();

                            psMov.setLong(1, idIng);
                            psMov.setLong(2, idLote);
                            psMov.setDouble(3, cant);
                            psMov.setString(4, "Consumo definitivo pedido #" + pedido.getIdPedido());
                            psMov.executeUpdate();

                            psStock.setLong(1, idIng);
                            double restante = 0;
                            try (ResultSet rsSt = psStock.executeQuery()) {
                                if (rsSt.next()) {
                                    restante = rsSt.getDouble("restante");
                                }
                            }

                            resumenDescuentos.add(String.format("   - %s: -%.3f %s (Stock restante en BD: %.3f %s)",
                                    nombre, cant, unidad, restante, unidad));
                        }
                    }
                }

                if (!huboReservas) {
                    aplicarDescuentoFefoDirecto(conn, pedido, resumenDescuentos);
                }

                conn.commit();

                System.out.println("[Inventario - Cierre Pedido #" + pedido.getIdPedido()
                        + "] Descuento definitivo confirmado en Supabase:");
                for (String linea : resumenDescuentos) {
                    System.out.println(linea);
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.out.println("[Simulación Inventario - Pedido #" + pedido.getIdPedido()
                    + "] Descuento simulado de ingredientes:");
            for (IngredienteRequerido ing : pedido.getIngredientes().values()) {
                System.out.printf("   - %s: -%.3f %s%n",
                        ing.getNombre(), ing.getCantidadRequerida(), ing.getUnidadMedida());
            }
        }
    }

    private void aplicarDescuentoFefoDirecto(Connection conn, PedidoPendiente pedido, List<String> resumen) throws SQLException {
        String sqlLotes = """
                SELECT id_lote, cantidad_disponible
                FROM inventario.lote
                WHERE id_ingrediente = ?
                  AND cantidad_disponible > 0
                  AND (fecha_vencimiento IS NULL OR fecha_vencimiento >= CURRENT_DATE)
                ORDER BY fecha_vencimiento ASC NULLS LAST, id_lote ASC
                FOR UPDATE
                """;

        String sqlDescontarLote = "UPDATE inventario.lote SET cantidad_disponible = cantidad_disponible - ? WHERE id_lote = ?";
        String sqlMov = "INSERT INTO inventario.movimiento (id_ingrediente, id_lote, tipo_movimiento, cantidad, motivo) VALUES (?, ?, 'CONSUMO', ?, ?)";

        try (PreparedStatement psLotes = conn.prepareStatement(sqlLotes);
             PreparedStatement psDesc = conn.prepareStatement(sqlDescontarLote);
             PreparedStatement psMov = conn.prepareStatement(sqlMov)) {

            for (IngredienteRequerido ing : pedido.getIngredientes().values()) {
                double pendiente = ing.getCantidadRequerida();
                psLotes.setLong(1, ing.getIdIngrediente());

                try (ResultSet rs = psLotes.executeQuery()) {
                    while (rs.next() && pendiente > 0) {
                        long idLote = rs.getLong("id_lote");
                        double disp = rs.getDouble("cantidad_disponible");
                        double tomar = Math.min(disp, pendiente);

                        psDesc.setDouble(1, tomar);
                        psDesc.setLong(2, idLote);
                        psDesc.executeUpdate();

                        psMov.setLong(1, ing.getIdIngrediente());
                        psMov.setLong(2, idLote);
                        psMov.setDouble(3, tomar);
                        psMov.setString(4, "Consumo directo pedido #" + pedido.getIdPedido());
                        psMov.executeUpdate();

                        pendiente -= tomar;
                    }
                }
                resumen.add(String.format("   - %s: -%.3f %s",
                        ing.getNombre(), ing.getCantidadRequerida(), ing.getUnidadMedida()));
            }
        }
    }

    /**
     * Consulta dinámicamente cuántas porciones de cada platillo se han pedido en los últimos 7 días.
     */
    public List<DemandaSemanalPlatillo> obtenerDemandaSemanalPlatillos() {
        List<DemandaSemanalPlatillo> demanda = new ArrayList<>();
        String sql = """
                SELECT pl.nombre,
                       COALESCE(SUM(pd.cantidad), 0)::int AS total_porciones
                FROM pedidos.platillo pl
                LEFT JOIN pedidos.pedido_detalle pd ON pd.id_platillo = pl.id_platillo
                LEFT JOIN pedidos.pedido p
                       ON p.id_pedido = pd.id_pedido
                      AND p.fecha_pedido >= now() - INTERVAL '7 days'
                      AND p.estado NOT IN ('RECHAZADO', 'CANCELADO')
                WHERE pl.estado = 'ACTIVO'
                GROUP BY pl.id_platillo, pl.nombre
                ORDER BY total_porciones DESC, pl.nombre ASC
                """;

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                demanda.add(new DemandaSemanalPlatillo(
                        rs.getString("nombre"),
                        rs.getInt("total_porciones")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[PedidoDAO] Error consultando demanda semanal de platillos: " + e.getMessage());
        }
        return demanda;
    }

    /**
     * Calcula el stock actual junto con el consumo comprometido y la proyección semanal
     * basada dinámicamente en la cantidad de platillos pedidos en los últimos 7 días.
     */
    public List<ItemStockPredictivo> obtenerStockConPrediccion() {
        List<ItemStockPredictivo> reporte = new ArrayList<>();
        String sql = """
                WITH stock_vigente AS (
                    SELECT id_ingrediente, COALESCE(SUM(cantidad_disponible), 0) AS disponible
                    FROM inventario.lote
                    WHERE fecha_vencimiento IS NULL OR fecha_vencimiento >= CURRENT_DATE
                    GROUP BY id_ingrediente
                ),
                consumo_en_curso AS (
                    SELECT pi.id_ingrediente, COALESCE(SUM(pi.cantidad_requerida), 0) AS comprometido
                    FROM pedidos.pedido_ingrediente pi
                    JOIN pedidos.pedido p ON p.id_pedido = pi.id_pedido
                    WHERE p.estado IN ('RECIBIDO', 'VALIDANDO_INVENTARIO', 'EN_PREPARACION')
                    GROUP BY pi.id_ingrediente
                ),
                platos_semanales AS (
                    SELECT pd.id_platillo,
                           pl.nombre AS nombre_platillo,
                           SUM(pd.cantidad)::int AS total_porciones
                    FROM pedidos.pedido_detalle pd
                    JOIN pedidos.pedido p ON p.id_pedido = pd.id_pedido
                    JOIN pedidos.platillo pl ON pl.id_platillo = pd.id_platillo
                    WHERE p.fecha_pedido >= now() - INTERVAL '7 days'
                      AND p.estado NOT IN ('RECHAZADO', 'CANCELADO')
                    GROUP BY pd.id_platillo, pl.nombre
                ),
                demanda_semanal_ingrediente AS (
                    SELECT pi.id_ingrediente,
                           COALESCE(SUM(ps.total_porciones * pi.cantidad), 0) AS gasto_semanal_platos,
                           COALESCE(SUM(ps.total_porciones), 0)::int AS platos_asociados,
                           STRING_AGG(ps.total_porciones || 'x ' || ps.nombre_platillo, ', ' ORDER BY ps.nombre_platillo) AS detalle_platos
                    FROM platos_semanales ps
                    JOIN pedidos.platillo_ingrediente pi ON pi.id_platillo = ps.id_platillo
                    GROUP BY pi.id_ingrediente
                )
                SELECT i.nombre,
                       i.unidad_medida,
                       i.stock_minimo,
                       COALESCE(sv.disponible, 0) AS disponible,
                       COALESCE(cc.comprometido, 0) AS comprometido,
                       COALESCE(dsi.gasto_semanal_platos, 0) AS gasto_semanal_platos,
                       COALESCE(dsi.platos_asociados, 0) AS platos_asociados,
                       dsi.detalle_platos
                FROM inventario.ingrediente i
                LEFT JOIN stock_vigente sv ON sv.id_ingrediente = i.id_ingrediente
                LEFT JOIN consumo_en_curso cc ON cc.id_ingrediente = i.id_ingrediente
                LEFT JOIN demanda_semanal_ingrediente dsi ON dsi.id_ingrediente = i.id_ingrediente
                WHERE i.estado = 'ACTIVO'
                ORDER BY i.nombre ASC
                """;

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                reporte.add(new ItemStockPredictivo(
                        rs.getString("nombre"),
                        rs.getString("unidad_medida"),
                        rs.getDouble("disponible"),
                        rs.getDouble("stock_minimo"),
                        rs.getDouble("comprometido"),
                        rs.getDouble("gasto_semanal_platos"),
                        rs.getInt("platos_asociados"),
                        rs.getString("detalle_platos")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[PedidoDAO] Error calculando inventario predictivo: " + e.getMessage());
        }
        return reporte;
    }

    public Map<String, String> obtenerResumenStockActual() {
        Map<String, String> stock = new LinkedHashMap<>();
        for (ItemStockPredictivo item : obtenerStockConPrediccion()) {
            stock.put(
                    item.nombre,
                    String.format("%.3f %s | Pred. 7d (%d platos): %.3f %s [%s]",
                            item.stockDisponible, item.unidadMedida,
                            item.platosSemanaAsociados,
                            item.gastoPredictivo7Dias, item.unidadMedida,
                            item.diagnostico)
            );
        }
        return stock;
    }
}