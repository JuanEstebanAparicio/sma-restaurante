package sma.restaurante.exception;

public class ValidacionInventarioException
        extends RuntimeException {

    public ValidacionInventarioException(
            String mensaje) {

        super(mensaje);
    }
}