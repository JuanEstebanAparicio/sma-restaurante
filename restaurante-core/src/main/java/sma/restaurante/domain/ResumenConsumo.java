package sma.restaurante.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Consumo de un ingrediente dentro de la ventana de analisis y fecha de su primer movimiento. */
public record ResumenConsumo(BigDecimal consumoTotal, LocalDateTime primerMovimiento) {
}