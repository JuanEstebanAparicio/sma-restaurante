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
import java.util.List;

public class PedidoDAO {

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
}