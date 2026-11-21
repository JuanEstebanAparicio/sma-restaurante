package sma.restaurante.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Reglas puras de stock: sin base de datos ni JADE, faciles de probar. */
public final class ReglasStock {

    private ReglasStock() {
    }

    /** Un lote esta vencido si su fecha es anterior a hoy (si vence hoy, aun sirve). */
    public static boolean estaVencido(Lote lote, LocalDate hoy) {
        return lote.getFechaVencimiento() != null
                && lote.getFechaVencimiento().isBefore(hoy);
    }

    /** Suma lo disponible en lotes que no han vencido. */
    public static BigDecimal stockVigente(List<Lote> lotes, LocalDate hoy) {
        return lotes.stream()
                .filter(l -> !estaVencido(l, hoy))
                .map(ReglasStock::disponible)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Suma lo que sigue guardado en lotes ya vencidos (merma pendiente de registrar). */
    public static BigDecimal stockVencido(List<Lote> lotes, LocalDate hoy) {
        return lotes.stream()
                .filter(l -> estaVencido(l, hoy))
                .map(ReglasStock::disponible)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Vencimiento mas cercano, dentro de los proximos 'dias', entre lotes vigentes con existencias. */
    public static Optional<LocalDate> proximoVencimiento(List<Lote> lotes, LocalDate hoy, int dias) {
        LocalDate limite = hoy.plusDays(dias);
        return lotes.stream()
                .filter(l -> l.getFechaVencimiento() != null)
                .filter(l -> !estaVencido(l, hoy))
                .filter(l -> disponible(l).signum() > 0)
                .map(Lote::getFechaVencimiento)
                .filter(f -> !f.isAfter(limite))
                .min(LocalDate::compareTo);
    }

    private static BigDecimal disponible(Lote lote) {
        return lote.getCantidadDisponible() == null
                ? BigDecimal.ZERO
                : lote.getCantidadDisponible();
    }
}