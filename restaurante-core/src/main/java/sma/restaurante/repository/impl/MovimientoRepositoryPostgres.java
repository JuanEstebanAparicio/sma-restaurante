package sma.restaurante.repository.impl;

import sma.restaurante.domain.Movimiento;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MovimientoRepositoryPostgres {

    private Movimiento mapearMovimiento(
            ResultSet rs)
            throws SQLException {

        return Movimiento.builder()
                .idMovimiento(
                        rs.getLong("id_movimiento"))
                .idIngrediente(
                        rs.getLong("id_ingrediente"))
                .idLote(
                        rs.getLong("id_lote"))
                .tipoMovimiento(
                        rs.getString("tipo_movimiento"))
                .cantidad(
                        rs.getBigDecimal("cantidad"))
                .fechaMovimiento(
                        rs.getTimestamp("fecha_movimiento")
                                .toLocalDateTime())
                .motivo(
                        rs.getString("motivo"))
                .build();
    }

}
