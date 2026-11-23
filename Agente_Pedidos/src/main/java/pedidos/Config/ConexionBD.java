package pedidos.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {

    private static final String DB_URL = System.getenv().getOrDefault(
            "SUPABASE_DB_URL",
            "jdbc:postgresql://aws-0-us-west-1.pooler.supabase.com:6543/postgres"
    );
    private static final String DB_USER = System.getenv().getOrDefault(
            "",
            ""
    );
    private static final String DB_PASSWORD = System.getenv().getOrDefault(
            "",
            ""
    );

    public static Connection obtenerConexion() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}