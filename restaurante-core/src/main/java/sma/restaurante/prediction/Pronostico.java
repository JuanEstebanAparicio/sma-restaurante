package sma.restaurante.prediction;

import sma.restaurante.domain.enums.NivelRiesgo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Estimacion de agotamiento de un ingrediente. diasRestantes es null si no hay consumo. */
public record Pronostico(
        Long idIngrediente,
        String nombre,
        String unidad,
        BigDecimal stockVigente,
        BigDecimal consumoDiario,
        BigDecimal diasRestantes,
        int diasObservados,
        NivelRiesgo riesgo) {

    /** Hay motivo para avisar: aun queda stock, pero se agotara pronto al ritmo actual. */
    public boolean requiereAlerta() {
        return stockVigente.signum() > 0
                && (riesgo == NivelRiesgo.CRITICO || riesgo == NivelRiesgo.ALTO);
    }

    public String mensaje() {
        return "Aunque quedan " + numero(stockVigente) + " " + unidad + " de " + nombre
                + ", segun el consumo observado en " + dias(diasObservados)
                + " se agotara " + plazo();
    }

    /** Plazo en dias enteros, redondeado hacia abajo: nunca promete mas tiempo del que hay. */
    private String plazo() {
        long enteros = diasRestantes.setScale(0, RoundingMode.FLOOR).longValue();
        return enteros < 1 ? "en menos de 1 dia" : "en aprox. " + dias(enteros);
    }

    private static String dias(long cantidad) {
        return cantidad + (cantidad == 1 ? " dia" : " dias");
    }

    private static String numero(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }
}