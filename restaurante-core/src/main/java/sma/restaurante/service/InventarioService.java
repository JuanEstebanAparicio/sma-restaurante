package sma.restaurante.service;

import sma.restaurante.domain.Ingrediente;
import sma.restaurante.repository.IngredienteRepository;

import java.util.List;
import java.util.Optional;

public class InventarioService {

    private final IngredienteRepository ingredienteRepository;

    public InventarioService(
            IngredienteRepository ingredienteRepository) {

        this.ingredienteRepository =
                ingredienteRepository;
    }

    public Ingrediente registrarIngrediente(
            Ingrediente ingrediente) {

        validarIngrediente(ingrediente);

        return ingredienteRepository
                .guardar(ingrediente);
    }

    public Optional<Ingrediente>
    buscarIngrediente(Long id) {

        return ingredienteRepository
                .buscarPorId(id);
    }

    public List<Ingrediente>
    listarIngredientes() {

        return ingredienteRepository
                .listarTodos();
    }

    public void inactivarIngrediente(Long id) {

        ingredienteRepository
                .eliminar(id);
    }

    private void validarIngrediente(
            Ingrediente ingrediente) {

        if (ingrediente.getNombre() == null
                || ingrediente.getNombre().isBlank()) {

            throw new IllegalArgumentException(
                    "Nombre obligatorio");
        }

        if (ingrediente.getUnidadMedida() == null
                || ingrediente.getUnidadMedida().isBlank()) {

            throw new IllegalArgumentException(
                    "Unidad obligatoria");
        }
    }
}