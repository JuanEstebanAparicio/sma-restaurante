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

public abstract class LoteRepositoryPostgres
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
}