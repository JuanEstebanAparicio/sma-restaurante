package pedidos.DataAccessObject;

import pedidos.Config.ConexionBD;
import pedidos.model.PedidoPendiente;
import pedidos.model.PlatilloDetalle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PedidoDAO {

    private Connection getConnection() throws SQLException {
        // Reemplaza por: return DatabaseConnection.getConnection(); si quieres usar la de restaurante-core
        return ConexionBD.obtenerConexion();
    }

    public List<PedidoPendiente> obtenerPedidosPendientes() {
        List<PedidoPendiente> pedidos = new ArrayList<>();

        String sqlPedidos = """
                SELECT id_pedido, mesa
                FROM pedidos.pedido
                WHERE estado = 'PENDIENTE'
                ORDER BY id_pedido ASC
                """;

        String sqlPlatillos = """
                SELECT p.id_platillo, p.nombre, pp.cantidad, p.precio
                FROM pedidos.pedido_platillo pp
                JOIN pedidos.platillo p ON pp.id_platillo = p.id_platillo
                WHERE pp.id_pedido = ?
                """;

        String sqlIngredientes = """
                SELECT i.id_ingrediente,
                       i.nombre AS ingrediente_nombre,
                       SUM(pi.cantidad_requerida * pp.cantidad) AS total_requerido
                FROM pedidos.pedido_platillo pp
                JOIN pedidos.platillo_ingrediente pi ON pp.id_platillo = pi.id_platillo
                JOIN inventario.ingrediente i ON pi.id_ingrediente = i.id_ingrediente
                WHERE pp.id_pedido = ?
                GROUP BY i.id_ingrediente, i.nombre
                ORDER BY i.id_ingrediente ASC
                """;

        try (Connection conn = getConnection();
             PreparedStatement psPedidos = conn.prepareStatement(sqlPedidos);
             ResultSet rsPedidos = psPedidos.executeQuery()) {

            while (rsPedidos.next()) {
                int pedidoId = rsPedidos.getInt("id_pedido");
                String mesa = rsPedidos.getString("mesa");
                PedidoPendiente pedido = new PedidoPendiente(pedidoId, mesa);

                try (PreparedStatement psPlat = conn.prepareStatement(sqlPlatillos)) {
                    psPlat.setInt(1, pedidoId);
                    try (ResultSet rsPlat = psPlat.executeQuery()) {
                        while (rsPlat.next()) {
                            pedido.agregarPlatillo(new PlatilloDetalle(
                                    rsPlat.getInt("id_platillo"),
                                    rsPlat.getString("nombre"),
                                    rsPlat.getInt("cantidad"),
                                    rsPlat.getDouble("precio")
                            ));
                        }
                    }
                }

                try (PreparedStatement psIng = conn.prepareStatement(sqlIngredientes)) {
                    psIng.setInt(1, pedidoId);
                    try (ResultSet rsIng = psIng.executeQuery()) {
                        while (rsIng.next()) {
                            pedido.agregarRequerimientoIngrediente(
                                    rsIng.getInt("id_ingrediente"),
                                    rsIng.getString("ingrediente_nombre"),
                                    rsIng.getDouble("total_requerido")
                            );
                        }
                    }
                }

                if (!pedido.getPlatillos().isEmpty() && !pedido.getIngredientesTotales().isEmpty()) {
                    pedidos.add(pedido);
                }
            }

        } catch (SQLException e) {
            System.err.println("[PedidoDAO] Error al leer pedidos pendientes: " + e.getMessage());
        }

        return pedidos;
    }

    public void actualizarEstado(int pedidoId, String nuevoEstado) {
        String sql = "UPDATE pedidos.pedido SET estado = ? WHERE id_pedido = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setInt(2, pedidoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[PedidoDAO] Error al actualizar estado del pedido #" + pedidoId + ": " + e.getMessage());
        }
    }
}