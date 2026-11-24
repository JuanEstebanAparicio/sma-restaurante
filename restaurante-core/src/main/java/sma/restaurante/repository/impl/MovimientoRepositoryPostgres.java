package sma.restaurante.repository.impl;

import sma.restaurante.datasource.Jdbc;
import sma.restaurante.domain.Movimiento;
import sma.restaurante.domain.ResumenConsumo;
import sma.restaurante.domain.enums.TipoMovimiento;
import sma.restaurante.repository.MovimientoRepository;
import sma.restaurante.util.Tiempo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MovimientoRepositoryPostgres implements MovimientoRepository {

    private static final String SELECT = """
            SELECT id_movimiento, id_ingrediente, id_lote, tipo_movimiento,
                   cantidad, fecha_movimiento, motivo
            FROM inventario.movimiento
            """;

    private static final String SQL_LISTAR = SELECT + "ORDER BY fecha_movimiento DESC";
    private static final String SQL_POR_ID = SELECT + "WHERE id_movimiento = ?";
    private static final String SQL_POR_INGREDIENTE =
            SELECT + "WHERE id_ingrediente = ? ORDER BY fecha_movimiento DESC";
    private static final String SQL_POR_TIPO =
            SELECT + "WHERE tipo_movimiento = ? ORDER BY fecha_movimiento DESC";
    private static final String SQL_POR_PERIODO =
            SELECT + "WHERE fecha_movimiento BETWEEN ? AND ? ORDER BY fecha_movimiento DESC";

    private static final String SQL_INSERTAR = """
            INSERT INTO inventario.movimiento
                (id_ingrediente, id_lote, tipo_movimiento, cantidad, fecha_movimiento, motivo)
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id_movimiento
            """;

    private static final String SQL_RESUMEN_CONSUMO = """
            SELECT id_ingrediente,
                   COALESCE(SUM(cantidad) FILTER (
                       WHERE tipo_movimiento = 'CONSUMO'
                         AND fecha_movimiento BETWEEN ? AND ?), 0) AS consumo_total,
                   MIN(fecha_movimiento) AS primer_movimiento
            FROM inventario.movimiento
            GROUP BY id_ingrediente
            """;

    @Override
    public Movimiento guardar(Movimiento movimiento) {

        LocalDateTime fecha = movimiento.getFechaMovimiento() != null
                ? movimiento.getFechaMovimiento()
                : Tiempo.ahora();

        return Jdbc.consultarUno(SQL_INSERTAR,
                        ps -> {
                            ps.setLong(1, movimiento.getIdIngrediente());
                            Jdbc.asignarLong(ps, 2, movimiento.getIdLote());
                            Jdbc.asignarEnum(ps, 3, movimiento.getTipoMovimiento());
                            ps.setBigDecimal(4, movimiento.getCantidad());
                            Jdbc.asignarInstante(ps, 5, fecha);
                            ps.setString(6, movimiento.getMotivo());
                        },
                        rs -> {
                            movimiento.setIdMovimiento(rs.getLong("id_movimiento"));
                            movimiento.setFechaMovimiento(fecha);
                            return movimiento;
                        })
                .orElseThrow(() -> new IllegalStateException("El INSERT no devolvio el id del movimiento"));
    }

    @Override
    public Optional<Movimiento> buscarPorId(Long id) {
        return Jdbc.consultarUno(SQL_POR_ID, ps -> ps.setLong(1, id), MovimientoRepositoryPostgres::mapear);
    }

    @Override
    public List<Movimiento> listarTodos() {
        return Jdbc.consultar(SQL_LISTAR, Jdbc.SIN_PARAMETROS, MovimientoRepositoryPostgres::mapear);
    }

    @Override
    public List<Movimiento> listarPorIngrediente(Long idIngrediente) {
        return Jdbc.consultar(SQL_POR_INGREDIENTE, ps -> ps.setLong(1, idIngrediente),
                MovimientoRepositoryPostgres::mapear);
    }

    @Override
    public List<Movimiento> listarPorTipo(TipoMovimiento tipo) {
        return Jdbc.consultar(SQL_POR_TIPO, ps -> Jdbc.asignarEnum(ps, 1, tipo),
                MovimientoRepositoryPostgres::mapear);
    }

    @Override
    public List<Movimiento> listarPorPeriodo(LocalDateTime desde, LocalDateTime hasta) {
        return Jdbc.consultar(SQL_POR_PERIODO,
                ps -> {
                    Jdbc.asignarInstante(ps, 1, desde);
                    Jdbc.asignarInstante(ps, 2, hasta);
                },
                MovimientoRepositoryPostgres::mapear);
    }

    @Override
    public Map<Long, ResumenConsumo> resumenConsumo(LocalDateTime desde, LocalDateTime hasta) {

        Map<Long, ResumenConsumo> resumenes = new HashMap<>();

        Jdbc.consultar(SQL_RESUMEN_CONSUMO,
                ps -> {
                    Jdbc.asignarInstante(ps, 1, desde);
                    Jdbc.asignarInstante(ps, 2, hasta);
                },
                rs -> resumenes.put(
                        rs.getLong("id_ingrediente"),
                        new ResumenConsumo(
                                rs.getBigDecimal("consumo_total"),
                                Jdbc.leerInstante(rs, "primer_movimiento"))));

        return resumenes;
    }

    private static Movimiento mapear(ResultSet rs) throws SQLException {
        return Movimiento.builder()
                .idMovimiento(rs.getLong("id_movimiento"))
                .idIngrediente(rs.getLong("id_ingrediente"))
                .idLote(Jdbc.leerLong(rs, "id_lote"))
                .tipoMovimiento(Jdbc.leerEnum(rs, "tipo_movimiento", TipoMovimiento.class))
                .cantidad(rs.getBigDecimal("cantidad"))
                .fechaMovimiento(Jdbc.leerInstante(rs, "fecha_movimiento"))
                .motivo(rs.getString("motivo"))
                .build();
    }
}