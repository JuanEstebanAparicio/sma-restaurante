package sma.restaurante.exception;

public class IngredienteDuplicadoException
        extends RuntimeException {

    public IngredienteDuplicadoException(
            String nombre) {

        super(
                "Ya existe el ingrediente: "
                        + nombre
        );
    }
}