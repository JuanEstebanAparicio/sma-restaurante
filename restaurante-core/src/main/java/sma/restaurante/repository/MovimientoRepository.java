package sma.restaurante.repository;

import sma.restaurante.domain.Movimiento;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MovimientoRepository {

    Movimiento guardar(Movimiento movimiento);

    Optional<Movimiento> buscarPorId(Long id);

    List<Movimiento> listarTodos();

    List<Movimiento> listarPorIngrediente(Long idIngrediente);

    List<Movimiento> listarPorTipo(String tipo);

    List<Movimiento> listarPorPeriodo(
            LocalDateTime desde,
            LocalDateTime hasta
    );
}