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
        throw new UnsupportedOperationException();
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