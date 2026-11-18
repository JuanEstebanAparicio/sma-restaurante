package sma.restaurante.repository;

import sma.restaurante.domain.Lote;

import java.util.List;
import java.util.Optional;

public interface LoteRepository {

    Lote guardar(Lote lote);

    Optional<Lote> buscarPorId(Long id);

    List<Lote> listarPorIngrediente(Long idIngrediente);

    List<Lote> listarTodos();

    void actualizar(Lote lote);

    void eliminar(Long id);
}