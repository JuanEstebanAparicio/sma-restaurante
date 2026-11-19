package sma.restaurante.repository.impl;

import sma.restaurante.datasource.DatabaseConnection;
import sma.restaurante.domain.Alerta;
import sma.restaurante.repository.AlertaRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AlertaRepositoryPostgres
        implements AlertaRepository {

    private Alerta mapearAlerta(ResultSet rs)
            throws SQLException {

        return Alerta.builder()
                .idAlerta(
                        rs.getLong("id_alerta"))
                .idIngrediente(
                        rs.getLong("id_ingrediente"))
                .tipoAlerta(
                        rs.getString("tipo_alerta"))
                .mensaje(
                        rs.getString("mensaje"))
                .activa(
                        rs.getBoolean("activa"))
                .fechaGeneracion(
                        rs.getTimestamp("fecha_generacion")
                                .toLocalDateTime())
                .fechaResolucion(
                        rs.getTimestamp("fecha_resolucion") == null
                                ? null
                                : rs.getTimestamp("fecha_resolucion")
                                .toLocalDateTime())
                .build();
    }
    @Override
    public Optional<Alerta> buscarPorId(Long id) {

        String sql = """
            SELECT *
            FROM inventario.alerta
            WHERE id_alerta = ?
            """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setLong(1, id);

            try (ResultSet rs =
                         ps.executeQuery()) {

                if (rs.next()) {

                    return Optional.of(
                            mapearAlerta(rs)
                    );
                }
            }

            return Optional.empty();

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Alerta> listarTodas() {

        List<Alerta> alertas =
                new ArrayList<>();

        String sql = """
            SELECT *
            FROM inventario.alerta
            ORDER BY fecha_generacion DESC
            """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement ps =
                        connection.prepareStatement(sql);

                ResultSet rs =
                        ps.executeQuery()
        ) {

            while (rs.next()) {

                alertas.add(
                        mapearAlerta(rs)
                );
            }

        } catch (Exception e) {

            throw new RuntimeException(e);
        }

        return alertas;
    }

    @Override
    public List<Alerta> listarPorIngrediente(
            Long idIngrediente) {

        List<Alerta> alertas =
                new ArrayList<>();

        String sql = """
            SELECT *
            FROM inventario.alerta
            WHERE id_ingrediente = ?
            ORDER BY fecha_generacion DESC
            """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setLong(1, idIngrediente);

            try (ResultSet rs =
                         ps.executeQuery()) {

                while (rs.next()) {

                    alertas.add(
                            mapearAlerta(rs)
                    );
                }
            }

        } catch (Exception e) {

            throw new RuntimeException(e);
        }

        return alertas;
    }


    @Override
    public Alerta guardar(Alerta alerta) {

        String sql = """
            INSERT INTO inventario.alerta
            (
                id_ingrediente,
                tipo_alerta,
                mensaje,
                activa
            )
            VALUES
            (
                ?, ?, ?, ?
            )
            RETURNING id_alerta
            """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setLong(1, alerta.getIdIngrediente());
            ps.setString(2, alerta.getTipoAlerta());
            ps.setString(3, alerta.getMensaje());
            ps.setBoolean(4, alerta.getActiva());

            try (ResultSet rs =
                         ps.executeQuery()) {

                if (rs.next()) {

                    alerta.setIdAlerta(
                            rs.getLong("id_alerta")
                    );
                }
            }

            return alerta;

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Alerta> listarActivas() {

        List<Alerta> alertas =
                new ArrayList<>();

        String sql = """
            SELECT *
            FROM inventario.alerta
            WHERE activa = true
            ORDER BY fecha_generacion DESC
            """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement ps =
                        connection.prepareStatement(sql);

                ResultSet rs =
                        ps.executeQuery()
        ) {

            while (rs.next()) {

                alertas.add(
                        mapearAlerta(rs)
                );
            }

        } catch (Exception e) {

            throw new RuntimeException(e);
        }

        return alertas;
    }

    @Override
    public void resolver(Long idAlerta) {

        String sql = """
            UPDATE inventario.alerta
            SET
                activa = false,
                fecha_resolucion = NOW()
            WHERE id_alerta = ?
            """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setLong(1, idAlerta);

            ps.executeUpdate();

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }
}
