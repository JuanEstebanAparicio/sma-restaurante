package sma.restaurante.repository.impl;

import sma.restaurante.datasource.Jdbc;
import sma.restaurante.domain.Alerta;
import sma.restaurante.domain.enums.TipoAlerta;
import sma.restaurante.repository.AlertaRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AlertaRepositoryPostgres implements AlertaRepository {

    private static final String SELECT = """
            SELECT id_alerta, id_ingrediente, tipo_alerta, mensaje, activa,
                   fecha_generacion, fecha_resolucion
            FROM inventario.alerta
            """;

    private static final String SQL_POR_ID = SELECT + "WHERE id_alerta = ?";
    private static final String SQL_LISTAR = SELECT + "ORDER BY fecha_generacion DESC";
    private static final String SQL_ACTIVAS = SELECT + "WHERE activa ORDER BY fecha_generacion DESC";
    private static final String SQL_POR_INGREDIENTE =
            SELECT + "WHERE id_ingrediente = ? ORDER BY fecha_generacion DESC";

    private static final String SQL_INSERTAR = """
            INSERT INTO inventario.alerta (id_ingrediente, tipo_alerta, mensaje, activa)
            VALUES (?, ?, ?, ?)
            RETURNING id_alerta, fecha_generacion
            """;

    /** Con el indice unico parcial uq_alerta_activa, un segundo aviso activo igual no se inserta. */
    private static final String SQL_INSERTAR_SI_NO_EXISTE = """
            INSERT INTO inventario.alerta (id_ingrediente, tipo_alerta, mensaje, activa)
            VALUES (?, ?, ?, TRUE)
            ON CONFLICT (id_ingrediente, tipo_alerta) WHERE activa DO NOTHING
            RETURNING id_alerta, fecha_generacion
            """;

    private static final String SQL_RESOLVER = """
            UPDATE inventario.alerta
            SET activa = FALSE, fecha_resolucion = NOW()
            WHERE id_alerta = ? AND activa
            """;

    @Override
    public Alerta guardar(Alerta alerta) {
        return Jdbc.consultarUno(SQL_INSERTAR,
                        ps -> {
                            asignarBase(ps, alerta);
                            ps.setBoolean(4, alerta.getActiva() == null || alerta.getActiva());
                        },
                        rs -> registrarGenerada(alerta, rs))
                .orElseThrow(() -> new IllegalStateException("El INSERT no devolvio el id de la alerta"));
    }

    @Override
    public boolean guardarSiNoExiste(Alerta alerta) {
        return Jdbc.consultarUno(SQL_INSERTAR_SI_NO_EXISTE,
                        ps -> asignarBase(ps, alerta),
                        rs -> registrarGenerada(alerta, rs))
                .isPresent();
    }

    @Override
    public Optional<Alerta> buscarPorId(Long id) {
        return Jdbc.consultarUno(SQL_POR_ID, ps -> ps.setLong(1, id), AlertaRepositoryPostgres::mapear);
    }

    @Override
    public List<Alerta> listarTodas() {
        return Jdbc.consultar(SQL_LISTAR, Jdbc.SIN_PARAMETROS, AlertaRepositoryPostgres::mapear);
    }

    @Override
    public List<Alerta> listarActivas() {
        return Jdbc.consultar(SQL_ACTIVAS, Jdbc.SIN_PARAMETROS, AlertaRepositoryPostgres::mapear);
    }

    @Override
    public List<Alerta> listarPorIngrediente(Long idIngrediente) {
        return Jdbc.consultar(SQL_POR_INGREDIENTE, ps -> ps.setLong(1, idIngrediente),
                AlertaRepositoryPostgres::mapear);
    }

    @Override
    public void resolver(Long idAlerta) {
        Jdbc.actualizar(SQL_RESOLVER, ps -> ps.setLong(1, idAlerta));
    }

    private static void asignarBase(PreparedStatement ps, Alerta alerta) throws SQLException {
        ps.setLong(1, alerta.getIdIngrediente());
        Jdbc.asignarEnum(ps, 2, alerta.getTipoAlerta());
        ps.setString(3, alerta.getMensaje());
    }

    private static Alerta registrarGenerada(Alerta alerta, ResultSet rs) throws SQLException {
        alerta.setIdAlerta(rs.getLong("id_alerta"));
        alerta.setFechaGeneracion(Jdbc.leerInstante(rs, "fecha_generacion"));
        return alerta;
    }

    private static Alerta mapear(ResultSet rs) throws SQLException {
        return Alerta.builder()
                .idAlerta(rs.getLong("id_alerta"))
                .idIngrediente(rs.getLong("id_ingrediente"))
                .tipoAlerta(Jdbc.leerEnum(rs, "tipo_alerta", TipoAlerta.class))
                .mensaje(rs.getString("mensaje"))
                .activa(rs.getBoolean("activa"))
                .fechaGeneracion(Jdbc.leerInstante(rs, "fecha_generacion"))
                .fechaResolucion(Jdbc.leerInstante(rs, "fecha_resolucion"))
                .build();
    }
}