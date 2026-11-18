package sma.restaurante.repository.impl;

import sma.restaurante.domain.Lote;
import sma.restaurante.repository.LoteRepository;

import java.util.List;
import java.util.Optional;

public class LoteRepositoryPostgres
        implements LoteRepository {

    @Override
    public Lote guardar(Lote lote) {
        return null;
    }

    @Override
    public Optional<Lote> buscarPorId(Long id) {
        return Optional.empty();
    }

    @Override
    public List<Lote> listarPorIngrediente(Long idIngrediente) {
        return List.of();
    }

    @Override
    public List<Lote> listarTodos() {
        return List.of();
    }

    @Override
    public void actualizar(Lote lote) {

    }

    @Override
    public void eliminar(Long id) {

    }
}

