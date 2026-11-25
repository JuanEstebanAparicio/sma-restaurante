package pedidos.Config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConexionBD {

    private static final Properties PROPIEDADES = cargarPropiedades();

    private ConexionBD() {
    }

    public static Connection obtenerConexion() throws SQLException {
        String url = obtenerValor("db.url", "SMA_DB_URL");
        String user = obtenerValor("db.user", "SMA_DB_USER");
        String password = obtenerValor("db.password", "SMA_DB_PASSWORD");

        return DriverManager.getConnection(url, user, password);
    }

    private static String obtenerValor(String clavePropiedad, String variableEntorno) {
        String desdeEntorno = System.getenv(variableEntorno);
        if (desdeEntorno != null && !desdeEntorno.isBlank()) {
            return desdeEntorno.trim();
        }

        String desdeArchivo = PROPIEDADES.getProperty(clavePropiedad);
        if (desdeArchivo != null && !desdeArchivo.isBlank()) {
            return desdeArchivo.trim();
        }

        throw new IllegalStateException(
                "Falta configurar '" + clavePropiedad + "' en Agente_Pedidos/src/main/resources/application.properties "
                        + "(o la variable de entorno " + variableEntorno + ")."
        );
    }

    private static Properties cargarPropiedades() {
        Properties props = new Properties();
        try (InputStream input = ConexionBD.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input != null) {
                props.load(input);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error leyendo el archivo application.properties", e);
        }
        return props;
    }
}