package sma.restaurante.exception;

import java.sql.SQLException;

/** Fallo al hablar con la base de datos. Conserva el SQLState para poder distinguir causas. */
public class PersistenciaException extends RuntimeException {

    private static final String UNICIDAD_VIOLADA = "23505";

    private final String sqlState;

    public PersistenciaException(String mensaje, SQLException causa) {
        super(mensaje + " [" + causa.getSQLState() + "]: " + causa.getMessage(), causa);
        this.sqlState = causa.getSQLState();
    }

    public String sqlState() {
        return sqlState;
    }

    public boolean esViolacionDeUnicidad() {
        return UNICIDAD_VIOLADA.equals(sqlState);
    }
}