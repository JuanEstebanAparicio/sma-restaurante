package sma.restaurante.repository.impl;

import sma.restaurante.datasource.DatabaseConnection;
import sma.restaurante.domain.Movimiento;
import sma.restaurante.repository.MovimientoRepository;
import sma.restaurante.domain.ResumenConsumo;
import java.util.HashMap;
import java.util.Map;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MovimientoRepositoryPostgres
        implements MovimientoRepository {

    private Movimiento mapearMovimiento(ResultSet rs) throws SQLException {

        long idLote = rs.getLong("id_lote");
        boolean sinLote = rs.wasNull();

        return Movimiento.builder()
                .idMovimiento(rs.getLong("id_movimiento"))
                .idIngrediente(rs.getLong("id_ingrediente"))
                .idLote(sinLote ? null : idLote)
                .tipoMovimiento(rs.getString("tipo_movimiento"))
                .cantidad(rs.getBigDecimal("cantidad"))
                .fechaMovimiento(rs.getTimestamp("fecha_movimiento").toLocalDateTime())
                .motivo(rs.getString("motivo"))
                .build();
    }

    @Override
    public Movimiento guardar(Movimiento movimiento) {

        String sql = """
            INSERT INTO inventario.movimiento
                (id_ingrediente, id_lote, tipo_movimiento, cantidad, fecha_movimiento, motivo)
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id_movimiento
            """;

        LocalDateTime fecha = movimiento.getFechaMovimiento() != null
                ? movimiento.getFechaMovimiento()
                : LocalDateTime.now();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, movimiento.getIdIngrediente());

            if (movimiento.getIdLote() != null) {
                ps.setLong(2, movimiento.getIdLote());
            } else {
                ps.setNull(2, Types.BIGINT);
            }

            ps.setString(3, movimiento.getTipoMovimiento());
            ps.setBigDecimal(4, movimiento.getCantidad());
            ps.setTimestamp(5, Timestamp.valueOf(fecha));
            ps.setString(6, movimiento.getMotivo());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    movimiento.setIdMovimiento(rs.getLong("id_movimiento"));
                    movimiento.setFechaMovimiento(fecha);
                }
            }

            return movimiento;

        } catch (SQLException e) {
            throw new RuntimeException("No se pudo guardar el movimiento", e);
        }
    }

    @Override
    public Optional<Movimiento> buscarPorId(
            Long id) {

        String sql = """
                SELECT *
                FROM inventario.movimiento
                WHERE id_movimiento = ?
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setLong(1, id);

            try (ResultSet rs =
                         ps.executeQuery()) {

                if (rs.next()) {

                    return Optional.of(
                            mapearMovimiento(rs)
                    );
                }
            }

            return Optional.empty();

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Movimiento> listarTodos() {

        List<Movimiento> movimientos =
                new ArrayList<>();

        String sql = """
                SELECT *
                FROM inventario.movimiento
                ORDER BY fecha_movimiento DESC
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement ps =
                        connection.prepareStatement(sql);

                ResultSet rs =
                        ps.executeQuery()
        ) {

            while (rs.next()) {

                movimientos.add(
                        mapearMovimiento(rs)
                );
            }

        } catch (Exception e) {

            throw new RuntimeException(e);
        }

        return movimientos;
    }

    @Override
    public List<Movimiento> listarPorIngrediente(
            Long idIngrediente) {

        List<Movimiento> movimientos =
                new ArrayList<>();

        String sql = """
                SELECT *
                FROM inventario.movimiento
                WHERE id_ingrediente = ?
                ORDER BY fecha_movimiento DESC
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setLong(1, idIngrediente);

            try (ResultSet rs =
                         ps.executeQuery()) {

                while (rs.next()) {

                    movimientos.add(
                            mapearMovimiento(rs)
                    );
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(e);
        }

        return movimientos;
    }

    @Override
    public List<Movimiento> listarPorTipo(
            String tipo) {

        List<Movimiento> movimientos =
                new ArrayList<>();

        String sql = """
                SELECT *
                FROM inventario.movimiento
                WHERE tipo_movimiento = ?
                ORDER BY fecha_movimiento DESC
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setString(1, tipo);

            try (ResultSet rs =
                         ps.executeQuery()) {

                while (rs.next()) {

                    movimientos.add(
                            mapearMovimiento(rs)
                    );
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(e);
        }

        return movimientos;
    }

    @Override
    public List<Movimiento> listarPorPeriodo(
            LocalDateTime desde,
            LocalDateTime hasta) {

        List<Movimiento> movimientos =
                new ArrayList<>();

        String sql = """
                SELECT *
                FROM inventario.movimiento
                WHERE fecha_movimiento
                    BETWEEN ? AND ?
                ORDER BY fecha_movimiento
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setTimestamp(
                    1,
                    Timestamp.valueOf(desde));

            ps.setTimestamp(
                    2,
                    Timestamp.valueOf(hasta));

            try (ResultSet rs =
                         ps.executeQuery()) {

                while (rs.next()) {

                    movimientos.add(
                            mapearMovimiento(rs)
                    );
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(e);
        }

        return movimientos;
    }

    @Override
    public Map<Long, ResumenConsumo> resumenConsumo(LocalDateTime desde, LocalDateTime hasta) {

        String sql = """
            SELECT id_ingrediente,
                   COALESCE(SUM(cantidad) FILTER (
                       WHERE tipo_movimiento = 'CONSUMO'
                         AND fecha_movimiento BETWEEN ? AND ?), 0) AS consumo_total,
                   MIN(fecha_movimiento) AS primer_movimiento
            FROM inventario.movimiento
            GROUP BY id_ingrediente
            """;

        Map<Long, ResumenConsumo> resumenes = new HashMap<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(desde));
            ps.setTimestamp(2, Timestamp.valueOf(hasta));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resumenes.put(
                            rs.getLong("id_ingrediente"),
                            new ResumenConsumo(
                                    rs.getBigDecimal("consumo_total"),
                                    rs.getTimestamp("primer_movimiento").toLocalDateTime()));
                }
            }

            return resumenes;

        } catch (SQLException e) {
            throw new RuntimeException("No se pudo calcular el resumen de consumo", e);
        }
    }
}