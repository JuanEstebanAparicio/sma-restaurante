package sma.restaurante.datasource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class DatabaseConfig {

    private static final Properties ARCHIVO = cargarArchivo();

    private DatabaseConfig() {
    }

    /** Prioridad: variable de entorno (db.url -> SMA_DB_URL) y luego application.properties. */
    public static String get(String clave) {
        String entorno = System.getenv(aVariableDeEntorno(clave));
        if (entorno != null && !entorno.isBlank()) {
            return entorno;
        }
        return ARCHIVO.getProperty(clave);
    }

    public static String requerida(String clave) {
        String valor = get(clave);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta la configuracion '" + clave
                    + "' (variable " + aVariableDeEntorno(clave) + " o application.properties)");
        }
        return valor;
    }

    public static int entero(String clave, int porDefecto) {
        String valor = get(clave);
        return (valor == null || valor.isBlank()) ? porDefecto : Integer.parseInt(valor.trim());
    }

    private static String aVariableDeEntorno(String clave) {
        return "SMA_" + clave.toUpperCase().replace('.', '_');
    }

    private static Properties cargarArchivo() {
        Properties propiedades = new Properties();
        try (InputStream in = DatabaseConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (in != null) {
                propiedades.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error leyendo application.properties", e);
        }
        return propiedades;
    }
}