package sma.restaurante.exception;

public class IngredienteNoEncontradoException
        extends RuntimeException {

    public IngredienteNoEncontradoException(Long id) {

        super(
                "No existe ingrediente con id: "
                        + id
        );
    }
}