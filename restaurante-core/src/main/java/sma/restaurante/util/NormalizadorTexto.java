package sma.restaurante.util;

public final class NormalizadorTexto {

    private NormalizadorTexto() {}

    public static String normalizar(
            String texto) {

        if (texto == null) {
            return null;
        }

        return texto
                .trim()
                .replaceAll("\\s+", " ")
                .toUpperCase();
    }
}