package sma.restaurante.memoria;

import sma.restaurante.domain.Movimiento;
import sma.restaurante.domain.ResumenConsumo;
import sma.restaurante.domain.enums.TipoMovimiento;
import sma.restaurante.repository.MovimientoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MovimientoRepositoryMemoria implements MovimientoRepository {

    private final List<Movimiento> datos = new ArrayList<>();
    private long siguiente = 1;

    @Override
    public Movimiento guardar(Movimiento movimiento) {
        movimiento.setIdMovimiento(siguiente++);
        if (movimiento.getFechaMovimiento() == null) {
            movimiento.setFechaMovimiento(LocalDateTime.now());
        }
        datos.add(movimiento);
        return movimiento;
    }

    @Override
    public Optional<Movimiento> buscarPorId(Long id) {
        return datos.stream().filter(m -> m.getIdMovimiento().equals(id)).findFirst();
    }

    @Override
    public List<Movimiento> listarTodos() {
        return new ArrayList<>(datos);
    }

    @Override
    public List<Movimiento> listarPorIngrediente(Long idIngrediente) {
        return datos.stream()
                .filter(m -> m.getIdIngrediente().equals(idIngrediente))
                .toList();
    }

    @Override
    public List<Movimiento> listarPorTipo(TipoMovimiento tipo) {
        return datos.stream().filter(m -> m.getTipoMovimiento().equals(tipo)).toList();
    }

    @Override
    public List<Movimiento> listarPorPeriodo(LocalDateTime desde, LocalDateTime hasta) {
        return datos.stream()
                .filter(m -> !m.getFechaMovimiento().isBefore(desde)
                        && !m.getFechaMovimiento().isAfter(hasta))
                .toList();
    }

    @Override
    public Map<Long, ResumenConsumo> resumenConsumo(LocalDateTime desde, LocalDateTime hasta) {

        Map<Long, ResumenConsumo> resumenes = new HashMap<>();

        for (Movimiento m : datos) {
            BigDecimal consumo = m.getTipoMovimiento() == TipoMovimiento.CONSUMO
                    && !m.getFechaMovimiento().isBefore(desde)
                    && !m.getFechaMovimiento().isAfter(hasta)
                    ? m.getCantidad()
                    : BigDecimal.ZERO;

            resumenes.merge(
                    m.getIdIngrediente(),
                    new ResumenConsumo(consumo, m.getFechaMovimiento()),
                    (a, b) -> new ResumenConsumo(
                            a.consumoTotal().add(b.consumoTotal()),
                            a.primerMovimiento().isBefore(b.primerMovimiento())
                                    ? a.primerMovimiento()
                                    : b.primerMovimiento()));
        }

        return resumenes;
    }
}