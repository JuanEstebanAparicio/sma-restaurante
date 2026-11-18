package sma.restaurante.repository;

import sma.restaurante.domain.Ingrediente;

import java.util.List;
import java.util.Optional;

public interface IngredienteRepository {

    Ingrediente guardar(Ingrediente ingrediente);

    Optional<Ingrediente> buscarPorId(Long id);

    Optional<Ingrediente> buscarPorNombre(String nombre);

    List<Ingrediente> listarTodos();

    void actualizar(Ingrediente ingrediente);

    void eliminar(Long id);
}
