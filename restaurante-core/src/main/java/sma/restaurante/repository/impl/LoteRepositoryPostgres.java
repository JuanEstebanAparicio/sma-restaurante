package sma.restaurante.repository.impl;

import sma.restaurante.datasource.DatabaseConnection;
import sma.restaurante.domain.Lote;
import sma.restaurante.repository.LoteRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LoteRepositoryPostgres
        implements LoteRepository {

    private Lote mapearLote(ResultSet rs)
            throws SQLException {

        return Lote.builder()
                .idLote(
                        rs.getLong("id_lote"))
                .idIngrediente(
                        rs.getLong("id_ingrediente"))
                .cantidad(
                        rs.getBigDecimal("cantidad"))
                .cantidadDisponible(
                        rs.getBigDecimal("cantidad_disponible"))
                .costoUnitario(
                        rs.getBigDecimal("costo_unitario"))
                .proveedor(
                        rs.getString("proveedor"))
                .ubicacion(
                        rs.getString("ubicacion"))
                .fechaIngreso(
                        rs.getDate("fecha_ingreso")
                                .toLocalDate())
                .fechaVencimiento(
                        rs.getDate("fecha_vencimiento")
                                != null
                                ? rs.getDate("fecha_vencimiento")
                                .toLocalDate()
                                : null)
                .build();
    }

    @Override
    public List<Lote> listarPorIngrediente(
            Long idIngrediente) {

        List<Lote> lotes =
                new ArrayList<>();

        String sql = """
            SELECT *
            FROM inventario.lote
            WHERE id_ingrediente = ?
            ORDER BY fecha_vencimiento
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

                    lotes.add(
                            mapearLote(rs)
                    );
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(e);
        }

        return lotes;
    }

    @Override
    public Optional<Lote> buscarPorId(Long id) {

        String sql = """
            SELECT *
            FROM inventario.lote
            WHERE id_lote = ?
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
                            mapearLote(rs)
                    );
                }
            }

            return Optional.empty();

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    @Override
    public Lote guardar(Lote lote) {

        String sql = """
            INSERT INTO inventario.lote
            (
                id_ingrediente,
                cantidad,
                cantidad_disponible,
                costo_unitario,
                proveedor,
                ubicacion,
                fecha_ingreso,
                fecha_vencimiento
            )
            VALUES
            (
                ?, ?, ?, ?, ?, ?, ?, ?
            )
            RETURNING id_lote
            """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setLong(1, lote.getIdIngrediente());
            ps.setBigDecimal(2, lote.getCantidad());
            ps.setBigDecimal(3, lote.getCantidadDisponible());
            ps.setBigDecimal(4, lote.getCostoUnitario());
            ps.setString(5, lote.getProveedor());
            ps.setString(6, lote.getUbicacion());

            ps.setDate(
                    7,
                    java.sql.Date.valueOf(
                            lote.getFechaIngreso()
                    ));

            if (lote.getFechaVencimiento() != null) {

                ps.setDate(
                        8,
                        java.sql.Date.valueOf(
                                lote.getFechaVencimiento()
                        )
                );

            } else {

                ps.setNull(
                        8,
                        java.sql.Types.DATE
                );
            }

            ResultSet rs =
                    ps.executeQuery();

            if (rs.next()) {

                lote.setIdLote(
                        rs.getLong("id_lote")
                );
            }

            return lote;

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Lote> listarTodos() {

        List<Lote> lotes =
                new ArrayList<>();

        String sql = """
            SELECT *
            FROM inventario.lote
            ORDER BY fecha_vencimiento
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

                lotes.add(
                        mapearLote(rs)
                );
            }

        } catch (Exception e) {

            throw new RuntimeException(e);
        }

        return lotes;
    }

    @Override
    public void actualizar(Lote lote) {

        String sql = """
            UPDATE inventario.lote
            SET
                cantidad = ?,
                cantidad_disponible = ?,
                costo_unitario = ?,
                proveedor = ?,
                ubicacion = ?,
                fecha_ingreso = ?,
                fecha_vencimiento = ?
            WHERE id_lote = ?
            """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setBigDecimal(
                    1,
                    lote.getCantidad());

            ps.setBigDecimal(
                    2,
                    lote.getCantidadDisponible());

            ps.setBigDecimal(
                    3,
                    lote.getCostoUnitario());

            ps.setString(
                    4,
                    lote.getProveedor());

            ps.setString(
                    5,
                    lote.getUbicacion());

            ps.setDate(
                    6,
                    java.sql.Date.valueOf(
                            lote.getFechaIngreso()));

            if (lote.getFechaVencimiento() != null) {

                ps.setDate(
                        7,
                        java.sql.Date.valueOf(
                                lote.getFechaVencimiento()));
            } else {

                ps.setNull(
                        7,
                        java.sql.Types.DATE);
            }

            ps.setLong(
                    8,
                    lote.getIdLote());

            ps.executeUpdate();

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    @Override
    public void eliminar(Long id) {

        String sql = """
            DELETE FROM inventario.lote
            WHERE id_lote = ?
            """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setLong(1, id);

            ps.executeUpdate();

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }
}