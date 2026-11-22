package sma.restaurante.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.function.Supplier;

public final class DatabaseConnection {

    private static volatile HikariDataSource dataSource;

    /** Conexion de la transaccion en curso en este hilo (null si no hay ninguna). */
    private static final ThreadLocal<Connection> TRANSACCION = new ThreadLocal<>();

    private DatabaseConnection() {
    }

    /**
     * Dentro de una transaccion devuelve siempre la misma conexion (y su close() no hace nada);
     * fuera de ella entrega una del pool, que close() devuelve al pool.
     */
    public static Connection getConnection() throws SQLException {
        Connection enCurso = TRANSACCION.get();
        if (enCurso != null) {
            return sinCierre(enCurso);
        }
        return pool().getConnection();
    }

    /** Ejecuta el trabajo en una transaccion: confirma si termina bien y revierte si falla. */
    public static <T> T enTransaccion(Supplier<T> trabajo) {

        if (TRANSACCION.get() != null) {
            return trabajo.get();   // ya hay una transaccion en este hilo: se une a ella
        }

        try (Connection conexion = pool().getConnection()) {

            conexion.setAutoCommit(false);
            TRANSACCION.set(conexion);

            try {
                T resultado = trabajo.get();
                conexion.commit();
                return resultado;

            } catch (Throwable fallo) {
                try {
                    conexion.rollback();
                } catch (SQLException e) {
                    fallo.addSuppressed(e);
                }
                if (fallo instanceof RuntimeException re) {
                    throw re;
                }
                if (fallo instanceof Error error) {
                    throw error;
                }
                throw new RuntimeException("Error en la transaccion", fallo);

            } finally {
                TRANSACCION.remove();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error en la transaccion", e);
        }
    }

    public static synchronized void cerrar() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    private static Connection sinCierre(Connection real) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class},
                (proxy, metodo, args) -> {
                    if ("close".equals(metodo.getName())) {
                        return null;
                    }
                    try {
                        return metodo.invoke(real, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
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
                    // La base y el programa deben hablar la misma hora (NOW() vs LocalDateTime.now())
                    String zona = DatabaseConfig.get("db.timezone");
                    config.setConnectionInitSql("SET TIME ZONE '"
                            + (zona == null || zona.isBlank() ? ZoneId.systemDefault().getId() : zona)
                            + "'");
                    dataSource = local = new HikariDataSource(config);
                }
            }
        }
        return local;
    }
}