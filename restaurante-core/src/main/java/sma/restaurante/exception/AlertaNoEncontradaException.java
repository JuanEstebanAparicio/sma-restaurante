package sma.restaurante.exception;

public class AlertaNoEncontradaException
        extends RuntimeException {

    public AlertaNoEncontradaException(Long id) {

        super(
                "No existe una alerta con ID: "
                        + id
        );
    }
}