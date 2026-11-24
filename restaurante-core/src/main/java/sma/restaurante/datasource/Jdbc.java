package sma.restaurante.datasource;

import sma.restaurante.exception.PersistenciaException;
import sma.restaurante.util.Tiempo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Ejecuta consultas SQL con la conexion de DatabaseConnection (que ya entiende de transacciones),
 * para que los repositorios solo digan "que SQL" y "como se mapea", sin repetir el manejo de recursos.
 */
public final class Jdbc {

    @FunctionalInterface
    public interface Parametros {
        void asignar(PreparedStatement ps) throws SQLException;
    }

    @FunctionalInterface
    public interface Mapeador<T> {
        T mapear(ResultSet rs) throws SQLException;
    }

    public static final Parametros SIN_PARAMETROS = ps -> {
    };

    private Jdbc() {
    }

    public static <T> List<T> consultar(String sql, Parametros parametros, Mapeador<T> mapeador) {
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            parametros.asignar(ps);

            try (ResultSet rs = ps.executeQuery()) {
                List<T> filas = new ArrayList<>();
                while (rs.next()) {
                    filas.add(mapeador.mapear(rs));
                }
                return filas;
            }

        } catch (SQLException e) {
            throw new PersistenciaException("Error en la consulta " + resumir(sql), e);
        }
    }

    /** Primera fila o vacio. Sirve tambien para INSERT ... RETURNING. */
    public static <T> Optional<T> consultarUno(String sql, Parametros parametros, Mapeador<T> mapeador) {
        List<T> filas = consultar(sql, parametros, mapeador);
        return filas.isEmpty() ? Optional.empty() : Optional.of(filas.get(0));
    }

    /** INSERT, UPDATE o DELETE sin RETURNING. Devuelve las filas afectadas. */
    public static int actualizar(String sql, Parametros parametros) {
        try (Connection conexion = DatabaseConnection.getConnection();
             PreparedStatement ps = conexion.prepareStatement(sql)) {

            parametros.asignar(ps);
            return ps.executeUpdate();

        } catch (SQLException e) {
            throw new PersistenciaException("Error en la sentencia " + resumir(sql), e);
        }
    }

    // ---------- lectura ----------

    public static Long leerLong(ResultSet rs, String columna) throws SQLException {
        long valor = rs.getLong(columna);
        return rs.wasNull() ? null : valor;
    }

    public static LocalDate leerFecha(ResultSet rs, String columna) throws SQLException {
        return rs.getObject(columna, LocalDate.class);
    }

    /** TIMESTAMPTZ -> hora local del restaurante. */
    public static LocalDateTime leerInstante(ResultSet rs, String columna) throws SQLException {
        return Tiempo.aLocal(rs.getObject(columna, OffsetDateTime.class));
    }

    public static <E extends Enum<E>> E leerEnum(ResultSet rs, String columna, Class<E> tipo) throws SQLException {
        String valor = rs.getString(columna);
        return valor == null ? null : Enum.valueOf(tipo, valor);
    }

    // ---------- escritura ----------

    public static void asignarLong(PreparedStatement ps, int indice, Long valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.BIGINT);
        } else {
            ps.setLong(indice, valor);
        }
    }

    public static void asignarFecha(PreparedStatement ps, int indice, LocalDate valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.DATE);
        } else {
            ps.setObject(indice, valor);
        }
    }

    /** Hora local del restaurante -> TIMESTAMPTZ. */
    public static void asignarInstante(PreparedStatement ps, int indice, LocalDateTime local) throws SQLException {
        if (local == null) {
            ps.setNull(indice, Types.TIMESTAMP_WITH_TIMEZONE);
        } else {
            ps.setObject(indice, Tiempo.aInstante(local));
        }
    }

    public static void asignarEnum(PreparedStatement ps, int indice, Enum<?> valor) throws SQLException {
        ps.setString(indice, valor == null ? null : valor.name());
    }

    private static String resumir(String sql) {
        String compacto = sql.strip().replaceAll("\\s+", " ");
        return compacto.length() > 80 ? compacto.substring(0, 80) + "..." : compacto;
    }
}