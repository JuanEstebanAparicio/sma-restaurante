package sma.restaurante.repository;

import sma.restaurante.domain.Alerta;

import java.util.List;
import java.util.Optional;

public interface AlertaRepository {

    Alerta guardar(Alerta alerta);

    Optional<Alerta> buscarPorId(Long id);

    List<Alerta> listarTodas();

    List<Alerta> listarActivas();

    List<Alerta> listarPorIngrediente(Long idIngrediente);

    boolean guardarSiNoExiste(Alerta alerta);

    void resolver(Long idAlerta);
}