package sma.restaurante.service;

import sma.restaurante.domain.Ingrediente;
import sma.restaurante.exception.ValidacionInventarioException;
import sma.restaurante.repository.IngredienteRepository;
import sma.restaurante.util.NormalizadorTexto;

public class InventarioService {

    private final IngredienteRepository
            ingredienteRepository;

    public InventarioService(
            IngredienteRepository ingredienteRepository) {

        this.ingredienteRepository =
                ingredienteRepository;
    }

    protected void validarIngrediente(
            Ingrediente ingrediente) {

        if (ingrediente.getNombre() == null
                || ingrediente.getNombre().isBlank()) {

            throw new ValidacionInventarioException(
                    "El nombre es obligatorio"
            );
        }

        if (ingrediente.getUnidadMedida() == null
                || ingrediente.getUnidadMedida().isBlank()) {

            throw new ValidacionInventarioException(
                    "La unidad de medida es obligatoria"
            );
        }
    }

    protected void normalizarIngrediente(
            Ingrediente ingrediente) {

        ingrediente.setNombre(
                NormalizadorTexto.normalizar(
                        ingrediente.getNombre()
                )
        );

        ingrediente.setCategoria(
                NormalizadorTexto.normalizar(
                        ingrediente.getCategoria()
                )
        );

        ingrediente.setEstado(
                NormalizadorTexto.normalizar(
                        ingrediente.getEstado()
                )
        );
    }
}