package sma.restaurante.exception;

public class LoteNoEncontradoException
        extends RuntimeException {

    public LoteNoEncontradoException(Long id) {

        super(
                "No existe un lote con ID: "
                        + id
        );
    }
}