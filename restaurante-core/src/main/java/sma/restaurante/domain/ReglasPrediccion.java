package sma.restaurante.domain;

import sma.restaurante.domain.enums.NivelRiesgo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/** Reglas puras para estimar cuando se agotara un ingrediente segun su consumo reciente. */
public final class ReglasPrediccion {

    /** Ventana de historial que se analiza (ultimos 21 dias). */
    public static final int DIAS_VENTANA = 21;

    private static final BigDecimal LIMITE_CRITICO = BigDecimal.valueOf(3);
    private static final BigDecimal LIMITE_ALTO = BigDecimal.valueOf(7);
    private static final BigDecimal LIMITE_MODERADO = BigDecimal.valueOf(15);

    private ReglasPrediccion() {
    }

    /**
     * Dias realmente observados: desde el primer movimiento hasta hoy (ambos inclusive),
     * con tope en la ventana y minimo 1. Evita subestimar el consumo de ingredientes nuevos.
     */
    public static int diasObservados(LocalDate primerMovimiento, LocalDate hoy) {
        if (primerMovimiento == null) {
            return 0;
        }
        long dias = ChronoUnit.DAYS.between(primerMovimiento, hoy) + 1;
        return (int) Math.max(1, Math.min(DIAS_VENTANA, dias));
    }

    public static BigDecimal consumoDiario(BigDecimal consumoTotal, int diasObservados) {
        if (consumoTotal == null || diasObservados <= 0) {
            return BigDecimal.ZERO;
        }
        return consumoTotal.divide(BigDecimal.valueOf(diasObservados), 3, RoundingMode.HALF_UP);
    }

    /** Vacio si no hay consumo: no se puede estimar cuando se agotara. */
    public static Optional<BigDecimal> diasRestantes(BigDecimal stock, BigDecimal consumoDiario) {
        if (consumoDiario == null || consumoDiario.signum() <= 0) {
            return Optional.empty();
        }
        return Optional.of(stock.divide(consumoDiario, 1, RoundingMode.HALF_UP));
    }

    public static NivelRiesgo nivel(Optional<BigDecimal> diasRestantes) {
        if (diasRestantes.isEmpty()) {
            return NivelRiesgo.SIN_CONSUMO;
        }
        BigDecimal dias = diasRestantes.get();
        if (dias.compareTo(LIMITE_CRITICO) <= 0) {
            return NivelRiesgo.CRITICO;
        }
        if (dias.compareTo(LIMITE_ALTO) <= 0) {
            return NivelRiesgo.ALTO;
        }
        if (dias.compareTo(LIMITE_MODERADO) <= 0) {
            return NivelRiesgo.MODERADO;
        }
        return NivelRiesgo.BAJO;
    }
}