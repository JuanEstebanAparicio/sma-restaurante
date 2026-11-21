package sma.restaurante.memoria;

import sma.restaurante.domain.Lote;
import sma.restaurante.repository.LoteRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LoteRepositoryMemoria implements LoteRepository {

    private final Map<Long, Lote> datos = new LinkedHashMap<>();
    private long siguiente = 1;

    @Override
    public Lote guardar(Lote lote) {
        lote.setIdLote(siguiente++);
        datos.put(lote.getIdLote(), lote);
        return lote;
    }

    @Override
    public Optional<Lote> buscarPorId(Long id) {
        return Optional.ofNullable(datos.get(id));
    }

    @Override
    public List<Lote> listarPorIngrediente(Long idIngrediente) {
        return datos.values().stream()
                .filter(l -> l.getIdIngrediente().equals(idIngrediente))
                .toList();
    }

    @Override
    public List<Lote> listarTodos() {
        return new ArrayList<>(datos.values());
    }

    @Override
    public void actualizar(Lote lote) {
        datos.put(lote.getIdLote(), lote);
    }

    @Override
    public void eliminar(Long id) {
        datos.remove(id);
    }
}