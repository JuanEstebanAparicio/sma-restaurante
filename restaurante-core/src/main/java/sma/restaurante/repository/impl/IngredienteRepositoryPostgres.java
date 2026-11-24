package sma.restaurante.repository.impl;

import sma.restaurante.datasource.Jdbc;
import sma.restaurante.domain.Ingrediente;
import sma.restaurante.domain.enums.EstadoIngrediente;
import sma.restaurante.exception.IngredienteDuplicadoException;
import sma.restaurante.exception.IngredienteNoEncontradoException;
import sma.restaurante.exception.PersistenciaException;
import sma.restaurante.repository.IngredienteRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class IngredienteRepositoryPostgres implements IngredienteRepository {

    private static final String SELECT = """
            SELECT id_ingrediente, nombre, descripcion, categoria, unidad_medida,
                   stock_minimo, estado, observaciones, fecha_creacion, fecha_actualizacion
            FROM inventario.ingrediente
            """;

    private static final String SQL_LISTAR = SELECT + "ORDER BY nombre";
    private static final String SQL_POR_ID = SELECT + "WHERE id_ingrediente = ?";
    private static final String SQL_POR_NOMBRE = SELECT + "WHERE nombre = ?";

    private static final String SQL_INSERTAR = """
            INSERT INTO inventario.ingrediente
                (nombre, descripcion, categoria, unidad_medida, stock_minimo, estado, observaciones)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING id_ingrediente, fecha_creacion, fecha_actualizacion
            """;

    private static final String SQL_ACTUALIZAR = """
            UPDATE inventario.ingrediente
            SET nombre = ?, descripcion = ?, categoria = ?, unidad_medida = ?,
                stock_minimo = ?, estado = ?, observaciones = ?, fecha_actualizacion = NOW()
            WHERE id_ingrediente = ?
            """;

    private static final String SQL_DESACTIVAR = """
            UPDATE inventario.ingrediente
            SET estado = 'INACTIVO', fecha_actualizacion = NOW()
            WHERE id_ingrediente = ?
            """;

    @Override
    public List<Ingrediente> listarTodos() {
        return Jdbc.consultar(SQL_LISTAR, Jdbc.SIN_PARAMETROS, IngredienteRepositoryPostgres::mapear);
    }

    @Override
    public Optional<Ingrediente> buscarPorId(Long id) {
        return Jdbc.consultarUno(SQL_POR_ID, ps -> ps.setLong(1, id), IngredienteRepositoryPostgres::mapear);
    }

    @Override
    public Optional<Ingrediente> buscarPorNombre(String nombre) {
        return Jdbc.consultarUno(SQL_POR_NOMBRE, ps -> ps.setString(1, nombre), IngredienteRepositoryPostgres::mapear);
    }

    @Override
    public Ingrediente guardar(Ingrediente ingrediente) {
        try {
            return Jdbc.consultarUno(SQL_INSERTAR,
                            ps -> asignarDatos(ps, ingrediente),
                            rs -> {
                                ingrediente.setIdIngrediente(rs.getLong("id_ingrediente"));
                                ingrediente.setFechaCreacion(Jdbc.leerInstante(rs, "fecha_creacion"));
                                ingrediente.setFechaActualizacion(Jdbc.leerInstante(rs, "fecha_actualizacion"));
                                return ingrediente;
                            })
                    .orElseThrow(() -> new IllegalStateException("El INSERT no devolvio el id del ingrediente"));

        } catch (PersistenciaException e) {
            // Dos registros simultaneos con el mismo nombre: la base de datos tiene la ultima palabra.
            if (e.esViolacionDeUnicidad()) {
                throw new IngredienteDuplicadoException(ingrediente.getNombre());
            }
            throw e;
        }
    }

    @Override
    public void actualizar(Ingrediente ingrediente) {
        Jdbc.actualizar(SQL_ACTUALIZAR, ps -> {
            asignarDatos(ps, ingrediente);
            ps.setLong(8, ingrediente.getIdIngrediente());
        });
    }

    /** Eliminacion logica: el ingrediente queda INACTIVO para conservar su historial. */
    @Override
    public void eliminar(Long id) {
        int filas = Jdbc.actualizar(SQL_DESACTIVAR, ps -> ps.setLong(1, id));
        if (filas == 0) {
            throw new IngredienteNoEncontradoException(id);
        }
    }

    private static void asignarDatos(PreparedStatement ps, Ingrediente ingrediente) throws SQLException {
        ps.setString(1, ingrediente.getNombre());
        ps.setString(2, ingrediente.getDescripcion());
        ps.setString(3, ingrediente.getCategoria());
        ps.setString(4, ingrediente.getUnidadMedida());
        ps.setBigDecimal(5, ingrediente.getStockMinimo());
        Jdbc.asignarEnum(ps, 6, ingrediente.getEstado());
        ps.setString(7, ingrediente.getObservaciones());
    }

    private static Ingrediente mapear(ResultSet rs) throws SQLException {
        return Ingrediente.builder()
                .idIngrediente(rs.getLong("id_ingrediente"))
                .nombre(rs.getString("nombre"))
                .descripcion(rs.getString("descripcion"))
                .categoria(rs.getString("categoria"))
                .unidadMedida(rs.getString("unidad_medida"))
                .stockMinimo(rs.getBigDecimal("stock_minimo"))
                .estado(Jdbc.leerEnum(rs, "estado", EstadoIngrediente.class))
                .observaciones(rs.getString("observaciones"))
                .fechaCreacion(Jdbc.leerInstante(rs, "fecha_creacion"))
                .fechaActualizacion(Jdbc.leerInstante(rs, "fecha_actualizacion"))
                .build();
    }
}