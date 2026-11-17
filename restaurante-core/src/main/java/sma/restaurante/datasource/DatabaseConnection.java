package sma.restaurante.datasource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {

        String url = DatabaseConfig.get("db.url");
        String user = DatabaseConfig.get("db.user");
        String password = DatabaseConfig.get("db.password");

        return DriverManager.getConnection(
                url,
                user,
                password
        );
    }
}