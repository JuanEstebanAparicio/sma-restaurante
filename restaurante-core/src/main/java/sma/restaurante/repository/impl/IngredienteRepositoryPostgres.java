package sma.restaurante.repository.impl;

import sma.restaurante.datasource.DatabaseConnection;
import sma.restaurante.domain.Ingrediente;
import sma.restaurante.repository.IngredienteRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IngredienteRepositoryPostgres
        implements IngredienteRepository {

    @Override
    public List<Ingrediente> listarTodos() {

        List<Ingrediente> ingredientes =
                new ArrayList<>();

        String sql = """
                SELECT *
                FROM inventario.ingrediente
                ORDER BY nombre
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

                Ingrediente ingrediente =
                        Ingrediente.builder()
                                .idIngrediente(
                                        rs.getLong("id_ingrediente"))
                                .nombre(
                                        rs.getString("nombre"))
                                .descripcion(
                                        rs.getString("descripcion"))
                                .categoria(
                                        rs.getString("categoria"))
                                .unidadMedida(
                                        rs.getString("unidad_medida"))
                                .stockMinimo(
                                        rs.getBigDecimal("stock_minimo"))
                                .estado(
                                        rs.getString("estado"))
                                .observaciones(
                                        rs.getString("observaciones"))
                                .build();

                ingredientes.add(
                        ingrediente
                );
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return ingredientes;
    }

    @Override
    public Ingrediente guardar(Ingrediente ingrediente) {

        String sql = """
            INSERT INTO inventario.ingrediente
            (
                nombre,
                descripcion,
                categoria,
                unidad_medida,
                stock_minimo,
                estado,
                observaciones
            )
            VALUES
            (
                ?, ?, ?, ?, ?, ?, ?
            )
            RETURNING id_ingrediente
            """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setString(1, ingrediente.getNombre());
            ps.setString(2, ingrediente.getDescripcion());
            ps.setString(3, ingrediente.getCategoria());
            ps.setString(4, ingrediente.getUnidadMedida());
            ps.setBigDecimal(5, ingrediente.getStockMinimo());
            ps.setString(6, ingrediente.getEstado());
            ps.setString(7, ingrediente.getObservaciones());

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                ingrediente.setIdIngrediente(
                        rs.getLong("id_ingrediente")
                );
            }

            return ingrediente;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Ingrediente> buscarPorId(Long id) {
        String sql = """
                SELECT *
                FROM inventario.ingrediente
                WHERE id_ingrediente = ?
                """;
        try(
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    Ingrediente ingrediente =
                            Ingrediente.builder()
                                    .idIngrediente(
                                            rs.getLong("id_ingrediente"))
                                    .nombre(
                                            rs.getString("nombre"))
                                    .descripcion(
                                            rs.getString("descripcion"))
                                    .categoria(
                                            rs.getString("categoria"))
                                    .unidadMedida(
                                            rs.getString("unidad_medida"))
                                    .stockMinimo(
                                            rs.getBigDecimal("stock_minimo"))
                                    .estado(
                                            rs.getString("estado"))
                                    .observaciones(
                                            rs.getString("observaciones"))
                                    .build();

                    return Optional.of(ingrediente);
                }
            }

            return Optional.empty();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Optional<Ingrediente> buscarPorNombre(String nombre) {
        String sql = """
                SELECT *
                FROM inventario.ingrediente
                WHERE nombre = ?
                """;
        try(
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setString(1, nombre);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    Ingrediente ingrediente =
                            Ingrediente.builder()
                                    .idIngrediente(
                                            rs.getLong("id_ingrediente"))
                                    .nombre(
                                            rs.getString("nombre"))
                                    .descripcion(
                                            rs.getString("descripcion"))
                                    .categoria(
                                            rs.getString("categoria"))
                                    .unidadMedida(
                                            rs.getString("unidad_medida"))
                                    .stockMinimo(
                                            rs.getBigDecimal("stock_minimo"))
                                    .estado(
                                            rs.getString("estado"))
                                    .observaciones(
                                            rs.getString("observaciones"))
                                    .build();

                    return Optional.of(ingrediente);
                }
            }

            return Optional.empty();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void actualizar(Ingrediente ingrediente) {

        String sql = """
            UPDATE inventario.ingrediente
            SET
                nombre = ?,
                descripcion = ?,
                categoria = ?,
                unidad_medida = ?,
                stock_minimo = ?,
                estado = ?,
                observaciones = ?
            WHERE id_ingrediente = ?
            """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setString(1, ingrediente.getNombre());
            ps.setString(2, ingrediente.getDescripcion());
            ps.setString(3, ingrediente.getCategoria());
            ps.setString(4, ingrediente.getUnidadMedida());
            ps.setBigDecimal(5, ingrediente.getStockMinimo());
            ps.setString(6, ingrediente.getEstado());
            ps.setString(7, ingrediente.getObservaciones());
            ps.setLong(8, ingrediente.getIdIngrediente());

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void eliminar(Long id) {

        String sql = """
            UPDATE inventario.ingrediente
            SET estado = 'INACTIVO'
            WHERE id_ingrediente = ?
            """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement ps =
                        connection.prepareStatement(sql)
        ) {

            ps.setLong(1, id);

            int filasAfectadas = ps.executeUpdate();

            if (filasAfectadas == 0) {
                throw new RuntimeException(
                        "No se encontró el ingrediente con ID: " + id
                );
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}