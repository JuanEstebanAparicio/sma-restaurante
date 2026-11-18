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
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Ingrediente> buscarPorNombre(String nombre) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void actualizar(Ingrediente ingrediente) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void eliminar(Long id) {
        throw new UnsupportedOperationException();
    }
}