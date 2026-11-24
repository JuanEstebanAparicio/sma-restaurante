package sma.restaurante.memoria;

import sma.restaurante.domain.Ingrediente;
import sma.restaurante.domain.enums.EstadoIngrediente;
import sma.restaurante.repository.IngredienteRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class IngredienteRepositoryMemoria implements IngredienteRepository {

    private final Map<Long, Ingrediente> datos = new LinkedHashMap<>();
    private long siguiente = 1;

    @Override
    public Ingrediente guardar(Ingrediente ingrediente) {
        ingrediente.setIdIngrediente(siguiente++);
        datos.put(ingrediente.getIdIngrediente(), ingrediente);
        return ingrediente;
    }

    @Override
    public Optional<Ingrediente> buscarPorId(Long id) {
        return Optional.ofNullable(datos.get(id));
    }

    @Override
    public Optional<Ingrediente> buscarPorNombre(String nombre) {
        return datos.values().stream()
                .filter(i -> i.getNombre().equals(nombre))
                .findFirst();
    }

    @Override
    public List<Ingrediente> listarTodos() {
        return new ArrayList<>(datos.values());
    }

    @Override
    public void actualizar(Ingrediente ingrediente) {
        datos.put(ingrediente.getIdIngrediente(), ingrediente);
    }

    @Override
    public void eliminar(Long id) {
        Ingrediente ingrediente = datos.get(id);
        if (ingrediente != null) {
            ingrediente.setEstado(EstadoIngrediente.INACTIVO);
        }
    }
}