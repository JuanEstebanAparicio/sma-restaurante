package sma.restaurante.repository;

import sma.restaurante.domain.Movimiento;
import sma.restaurante.domain.ResumenConsumo;
import sma.restaurante.domain.enums.TipoMovimiento;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MovimientoRepository {

    Movimiento guardar(Movimiento movimiento);

    Optional<Movimiento> buscarPorId(Long id);

    List<Movimiento> listarTodos();

    List<Movimiento> listarPorIngrediente(Long idIngrediente);

    List<Movimiento> listarPorTipo(TipoMovimiento tipo);

    List<Movimiento> listarPorPeriodo(
            LocalDateTime desde,
            LocalDateTime hasta
    );

    /**
     * Por ingrediente: consumo dentro de [desde, hasta] y fecha de su primer movimiento.
     * Una sola consulta para todos los ingredientes.
     */
    Map<Long, ResumenConsumo> resumenConsumo(LocalDateTime desde, LocalDateTime hasta);
}