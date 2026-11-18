package sma.restaurante.exception;

public class AlertaInventarioException
        extends RuntimeException {

    public AlertaInventarioException(
            String mensaje) {

        super(mensaje);
    }
}