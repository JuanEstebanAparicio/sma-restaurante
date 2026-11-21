package sma.restaurante.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public final class DatabaseConnection {

    private static volatile HikariDataSource dataSource;

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return pool().getConnection();
    }

    public static synchronized void cerrar() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    private static HikariDataSource pool() {
        HikariDataSource local = dataSource;
        if (local == null) {
            synchronized (DatabaseConnection.class) {
                local = dataSource;
                if (local == null) {
                    HikariConfig config = new HikariConfig();
                    config.setJdbcUrl(DatabaseConfig.requerida("db.url"));
                    config.setUsername(DatabaseConfig.requerida("db.user"));
                    config.setPassword(DatabaseConfig.requerida("db.password"));
                    config.setMaximumPoolSize(DatabaseConfig.entero("db.pool.max", 5));
                    config.setMinimumIdle(1);
                    config.setConnectionTimeout(10_000);
                    config.setPoolName("inventario-pool");
                    dataSource = local = new HikariDataSource(config);
                }
            }
        }
        return local;
    }
}