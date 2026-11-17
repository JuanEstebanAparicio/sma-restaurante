package sma.restaurante.datasource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DatabaseConfig {

    private static final Properties PROPERTIES = new Properties();

    static {

        try (InputStream inputStream =
                     DatabaseConfig.class
                             .getClassLoader()
                             .getResourceAsStream("application.properties")) {

            if (inputStream == null) {
                throw new RuntimeException(
                        "No se encontró application.properties"
                );
            }

            PROPERTIES.load(inputStream);

        } catch (IOException e) {
            throw new RuntimeException(
                    "Error leyendo application.properties",
                    e
            );
        }
    }

    public static String get(String key) {
        return PROPERTIES.getProperty(key);
    }
}