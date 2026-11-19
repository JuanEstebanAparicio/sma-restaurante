package sma.restaurante.prediction;

import sma.restaurante.domain.Lote;
import sma.restaurante.domain.Movimiento;
import sma.restaurante.repository.LoteRepository;
import sma.restaurante.repository.MovimientoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

public class PrediccionService {

    private static final int DIAS_HISTORICOS = 21;

    private final LoteRepository loteRepository;

    private final MovimientoRepository movimientoRepository;

    public PrediccionService(
            LoteRepository loteRepository,
            MovimientoRepository movimientoRepository) {

        this.loteRepository = loteRepository;
        this.movimientoRepository = movimientoRepository;
    }

    public BigDecimal calcularStockDisponible(
            Long idIngrediente) {

        return loteRepository
                .listarPorIngrediente(
                        idIngrediente
                )
                .stream()
                .map(
                        Lote::getCantidadDisponible
                )
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );
    }

    public BigDecimal calcularConsumoPromedioDiario(
            Long idIngrediente) {

        LocalDateTime desde =
                LocalDateTime.now()
                        .minusDays(
                                DIAS_HISTORICOS
                        );

        LocalDateTime hasta =
                LocalDateTime.now();

        List<Movimiento> movimientos =

                movimientoRepository
                        .listarPorPeriodo(
                                desde,
                                hasta
                        );

        BigDecimal consumoTotal =

                movimientos.stream()
                        .filter(m ->
                                m.getIdIngrediente()
                                        .equals(idIngrediente))
                        .filter(m ->
                                "CONSUMO".equals(
                                        m.getTipoMovimiento()))
                        .map(
                                Movimiento::getCantidad)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        return consumoTotal.divide(
                BigDecimal.valueOf(
                        DIAS_HISTORICOS
                ),
                3,
                RoundingMode.HALF_UP
        );
    }

    public BigDecimal calcularDiasRestantes(
            Long idIngrediente) {

        BigDecimal stockActual =
                calcularStockDisponible(
                        idIngrediente
                );

        BigDecimal consumoPromedio =

                calcularConsumoPromedioDiario(
                        idIngrediente
                );

        if (consumoPromedio.compareTo(
                BigDecimal.ZERO) <= 0) {

            return BigDecimal.valueOf(
                    9999
            );
        }

        return stockActual.divide(
                consumoPromedio,
                2,
                RoundingMode.HALF_UP
        );
    }

    public String predecirAgotamiento(
            Long idIngrediente,
            String nombreIngrediente,
            String unidadMedida) {

        BigDecimal stockActual =
                calcularStockDisponible(idIngrediente);

        BigDecimal consumoPromedio =
                calcularConsumoPromedioDiario(idIngrediente);

        BigDecimal diasRestantes =
                calcularDiasRestantes(idIngrediente);

        String nivelRiesgo;

        if (diasRestantes.compareTo(
                BigDecimal.valueOf(3)) <= 0) {

            nivelRiesgo = "CRÍTICO";

        } else if (diasRestantes.compareTo(
                BigDecimal.valueOf(7)) <= 0) {

            nivelRiesgo = "ALTO";

        } else if (diasRestantes.compareTo(
                BigDecimal.valueOf(15)) <= 0) {

            nivelRiesgo = "MODERADO";

        } else {

            nivelRiesgo = "BAJO";
        }

        return """
            ===== PREDICCIÓN DE INVENTARIO =====

            Ingrediente: %s

            Stock disponible: %s %s

            Consumo promedio diario: %s %s

            Tiempo estimado hasta agotamiento: %s días

            Nivel de riesgo: %s

            Recomendación:
            Se recomienda supervisar el comportamiento del inventario y planificar la reposición antes de que se alcance un nivel crítico.
            """
                .formatted(
                        nombreIngrediente,
                        stockActual,
                        unidadMedida,
                        consumoPromedio,
                        unidadMedida,
                        diasRestantes,
                        nivelRiesgo
                );
    }


}