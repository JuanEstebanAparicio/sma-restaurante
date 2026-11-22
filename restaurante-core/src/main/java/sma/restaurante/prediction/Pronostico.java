package sma.restaurante.prediction;

import sma.restaurante.domain.enums.NivelRiesgo;

import java.math.BigDecimal;

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
                + ", segun el consumo de los ultimos " + diasObservados
                + " dias se agotara en aprox. " + numero(diasRestantes) + " dias";
    }

    private static String numero(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }
}