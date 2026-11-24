package sma.restaurante.repository.impl;

import sma.restaurante.datasource.Jdbc;
import sma.restaurante.domain.Lote;
import sma.restaurante.repository.LoteRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class LoteRepositoryPostgres implements LoteRepository {

    private static final String SELECT = """
            SELECT id_lote, id_ingrediente, cantidad, cantidad_disponible, costo_unitario,
                   proveedor, ubicacion, fecha_ingreso, fecha_vencimiento
            FROM inventario.lote
            """;

    private static final String SQL_LISTAR = SELECT + "ORDER BY fecha_vencimiento";
    private static final String SQL_POR_ID = SELECT + "WHERE id_lote = ?";
    private static final String SQL_POR_INGREDIENTE =
            SELECT + "WHERE id_ingrediente = ? ORDER BY fecha_vencimiento";

    private static final String SQL_INSERTAR = """
            INSERT INTO inventario.lote
                (id_ingrediente, cantidad, cantidad_disponible, costo_unitario,
                 proveedor, ubicacion, fecha_ingreso, fecha_vencimiento)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id_lote
            """;

    private static final String SQL_ACTUALIZAR = """
            UPDATE inventario.lote
            SET cantidad = ?, cantidad_disponible = ?, costo_unitario = ?,
                proveedor = ?, ubicacion = ?, fecha_ingreso = ?, fecha_vencimiento = ?
            WHERE id_lote = ?
            """;

    private static final String SQL_ELIMINAR = "DELETE FROM inventario.lote WHERE id_lote = ?";

    @Override
    public List<Lote> listarTodos() {
        return Jdbc.consultar(SQL_LISTAR, Jdbc.SIN_PARAMETROS, LoteRepositoryPostgres::mapear);
    }

    @Override
    public Optional<Lote> buscarPorId(Long id) {
        return Jdbc.consultarUno(SQL_POR_ID, ps -> ps.setLong(1, id), LoteRepositoryPostgres::mapear);
    }

    @Override
    public List<Lote> listarPorIngrediente(Long idIngrediente) {
        return Jdbc.consultar(SQL_POR_INGREDIENTE, ps -> ps.setLong(1, idIngrediente), LoteRepositoryPostgres::mapear);
    }

    @Override
    public Lote guardar(Lote lote) {
        return Jdbc.consultarUno(SQL_INSERTAR,
                        ps -> {
                            ps.setLong(1, lote.getIdIngrediente());
                            asignarDatos(ps, lote, 2);
                        },
                        rs -> {
                            lote.setIdLote(rs.getLong("id_lote"));
                            return lote;
                        })
                .orElseThrow(() -> new IllegalStateException("El INSERT no devolvio el id del lote"));
    }

    @Override
    public void actualizar(Lote lote) {
        Jdbc.actualizar(SQL_ACTUALIZAR, ps -> {
            asignarDatos(ps, lote, 1);
            ps.setLong(8, lote.getIdLote());
        });
    }

    @Override
    public void eliminar(Long id) {
        Jdbc.actualizar(SQL_ELIMINAR, ps -> ps.setLong(1, id));
    }

    /** Siete columnas consecutivas a partir de 'desde': cantidad ... fecha_vencimiento. */
    private static void asignarDatos(PreparedStatement ps, Lote lote, int desde) throws SQLException {
        ps.setBigDecimal(desde, lote.getCantidad());
        ps.setBigDecimal(desde + 1, lote.getCantidadDisponible());
        ps.setBigDecimal(desde + 2, lote.getCostoUnitario());
        ps.setString(desde + 3, lote.getProveedor());
        ps.setString(desde + 4, lote.getUbicacion());
        Jdbc.asignarFecha(ps, desde + 5, lote.getFechaIngreso());
        Jdbc.asignarFecha(ps, desde + 6, lote.getFechaVencimiento());
    }

    private static Lote mapear(ResultSet rs) throws SQLException {
        return Lote.builder()
                .idLote(rs.getLong("id_lote"))
                .idIngrediente(rs.getLong("id_ingrediente"))
                .cantidad(rs.getBigDecimal("cantidad"))
                .cantidadDisponible(rs.getBigDecimal("cantidad_disponible"))
                .costoUnitario(rs.getBigDecimal("costo_unitario"))
                .proveedor(rs.getString("proveedor"))
                .ubicacion(rs.getString("ubicacion"))
                .fechaIngreso(Jdbc.leerFecha(rs, "fecha_ingreso"))
                .fechaVencimiento(Jdbc.leerFecha(rs, "fecha_vencimiento"))
                .build();
    }
}