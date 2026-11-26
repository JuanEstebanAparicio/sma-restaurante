package pedidos.DataAccessObject;

import pedidos.Config.ConexionBD;
import pedidos.model.CuentaPedido;
import pedidos.model.ItemConsumo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class CuentaPagoDAO {

    public Optional<CuentaPedido> registrarConsumosYGenerarCuenta(long idPedido, String mesa) {
        String sqlDetalle = """
                SELECT pl.nombre, pd.cantidad, pd.precio_unitario
                FROM pedidos.pedido_detalle pd
                JOIN pedidos.platillo pl ON pd.id_platillo = pl.id_platillo
                WHERE pd.id_pedido = ?
                ORDER BY pl.id_platillo ASC
                """;

        String sqlUpsertCuenta = """
                INSERT INTO pagos.cuenta (id_pedido, mesa, subtotal, impuesto, total, estado)
                VALUES (?, ?, ?, 0, ?, 'PENDIENTE_PAGO')
                ON CONFLICT (id_pedido) DO UPDATE
                    SET mesa = EXCLUDED.mesa,
                        subtotal = EXCLUDED.subtotal,
                        total = EXCLUDED.total,
                        fecha_actualizacion = now()
                RETURNING id_cuenta, subtotal, impuesto, total, estado
                """;

        try (Connection conn = ConexionBD.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                CuentaPedido cuentaTemporal = new CuentaPedido(0, idPedido, mesa, 0, 0, 0, "PENDIENTE_PAGO");
                double subtotalCalculado = 0.0;

                try (PreparedStatement psDet = conn.prepareStatement(sqlDetalle)) {
                    psDet.setLong(1, idPedido);
                    try (ResultSet rs = psDet.executeQuery()) {
                        while (rs.next()) {
                            ItemConsumo item = new ItemConsumo(
                                    rs.getString("nombre"),
                                    rs.getInt("cantidad"),
                                    rs.getDouble("precio_unitario")
                            );
                            cuentaTemporal.agregarItem(item);
                            subtotalCalculado += item.getSubtotal();
                        }
                    }
                }

                if (cuentaTemporal.getItems().isEmpty()) {
                    conn.rollback();
                    return Optional.empty();
                }

                CuentaPedido cuentaFinal;
                try (PreparedStatement psCuenta = conn.prepareStatement(sqlUpsertCuenta)) {
                    psCuenta.setLong(1, idPedido);
                    psCuenta.setString(2, mesa);
                    psCuenta.setDouble(3, subtotalCalculado);
                    psCuenta.setDouble(4, subtotalCalculado);

                    try (ResultSet rsCuenta = psCuenta.executeQuery()) {
                        rsCuenta.next();
                        cuentaFinal = new CuentaPedido(
                                rsCuenta.getLong("id_cuenta"),
                                idPedido,
                                mesa,
                                rsCuenta.getDouble("subtotal"),
                                rsCuenta.getDouble("impuesto"),
                                rsCuenta.getDouble("total"),
                                rsCuenta.getString("estado")
                        );
                        cuentaTemporal.getItems().forEach(cuentaFinal::agregarItem);
                    }
                }

                conn.commit();
                return Optional.of(cuentaFinal);

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("[CuentaPagoDAO] Error generando cuenta para pedido #" + idPedido + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Double> procesarPago(long idPedido, String metodoPago) {
        String sqlBuscar = """
                SELECT id_cuenta, total, estado
                FROM pagos.cuenta
                WHERE id_pedido = ?
                FOR UPDATE
                """;

        String sqlInsertPago = """
                INSERT INTO pagos.pago (id_cuenta, metodo_pago, monto_pagado, referencia)
                VALUES (?, ?, ?, ?)
                """;

        String sqlCerrarCuenta = """
                UPDATE pagos.cuenta
                SET estado = 'PAGADA', fecha_actualizacion = now()
                WHERE id_cuenta = ?
                """;

        try (Connection conn = ConexionBD.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                long idCuenta;
                double total;
                String estado;

                try (PreparedStatement ps = conn.prepareStatement(sqlBuscar)) {
                    ps.setLong(1, idPedido);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return Optional.empty();
                        }
                        idCuenta = rs.getLong("id_cuenta");
                        total = rs.getDouble("total");
                        estado = rs.getString("estado");
                    }
                }

                if ("PAGADA".equalsIgnoreCase(estado)) {
                    conn.commit();
                    return Optional.of(total);
                }

                try (PreparedStatement psPago = conn.prepareStatement(sqlInsertPago)) {
                    psPago.setLong(1, idCuenta);
                    psPago.setString(2, metodoPago.toUpperCase());
                    psPago.setDouble(3, total);
                    psPago.setString(4, "PAGO-PED-" + idPedido);
                    psPago.executeUpdate();
                }

                try (PreparedStatement psUpd = conn.prepareStatement(sqlCerrarCuenta)) {
                    psUpd.setLong(1, idCuenta);
                    psUpd.executeUpdate();
                }

                conn.commit();
                return Optional.of(total);

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("[CuentaPagoDAO] Error procesando pago del pedido #" + idPedido + ": " + e.getMessage());
            return Optional.empty();
        }
    }
}