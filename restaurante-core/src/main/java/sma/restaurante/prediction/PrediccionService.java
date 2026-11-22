package sma.restaurante.prediction;

import sma.restaurante.domain.Ingrediente;
import sma.restaurante.domain.Lote;
import sma.restaurante.domain.ReglasPrediccion;
import sma.restaurante.domain.ReglasStock;
import sma.restaurante.domain.ResumenConsumo;
import sma.restaurante.repository.MovimientoRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PrediccionService {

    private final MovimientoRepository movimientoRepository;

    public PrediccionService(MovimientoRepository movimientoRepository) {
        this.movimientoRepository = movimientoRepository;
    }

    /**
     * Calcula un pronostico por ingrediente con una sola consulta de movimientos.
     * Recibe los ingredientes y lotes ya cargados para no repetir consultas.
     */
    public List<Pronostico> pronosticar(
            List<Ingrediente> ingredientes,
            Map<Long, List<Lote>> lotesPorIngrediente,
            LocalDateTime ahora) {

        LocalDate hoy = ahora.toLocalDate();

        Map<Long, ResumenConsumo> resumenes = movimientoRepository.resumenConsumo(
                ahora.minusDays(ReglasPrediccion.DIAS_VENTANA), ahora);

        List<Pronostico> pronosticos = new ArrayList<>();

        for (Ingrediente ingrediente : ingredientes) {

            Long id = ingrediente.getIdIngrediente();
            List<Lote> lotes = lotesPorIngrediente.getOrDefault(id, List.of());
            BigDecimal stock = ReglasStock.stockVigente(lotes, hoy);

            ResumenConsumo resumen = resumenes.get(id);

            int dias = 0;
            BigDecimal consumoDiario = BigDecimal.ZERO;
            if (resumen != null && resumen.primerMovimiento() != null) {
                dias = ReglasPrediccion.diasObservados(resumen.primerMovimiento().toLocalDate(), hoy);
                consumoDiario = ReglasPrediccion.consumoDiario(resumen.consumoTotal(), dias);
            }

            Optional<BigDecimal> diasRestantes = ReglasPrediccion.diasRestantes(stock, consumoDiario);

            pronosticos.add(new Pronostico(
                    id,
                    ingrediente.getNombre(),
                    ingrediente.getUnidadMedida(),
                    stock,
                    consumoDiario,
                    diasRestantes.orElse(null),
                    dias,
                    ReglasPrediccion.nivel(diasRestantes)));
        }

        return pronosticos;
    }
}