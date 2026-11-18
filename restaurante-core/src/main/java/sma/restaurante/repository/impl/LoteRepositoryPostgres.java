package sma.restaurante.repository.impl;

import sma.restaurante.domain.Lote;
import sma.restaurante.repository.LoteRepository;

import java.sql.ResultSet;
import java.sql.SQLException;

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
}