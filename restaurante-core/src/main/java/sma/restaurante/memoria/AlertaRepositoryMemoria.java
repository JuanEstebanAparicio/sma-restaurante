package sma.restaurante.memoria;

import sma.restaurante.domain.Alerta;
import sma.restaurante.repository.AlertaRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AlertaRepositoryMemoria implements AlertaRepository {

    private final Map<Long, Alerta> datos = new LinkedHashMap<>();
    private long siguiente = 1;

    @Override
    public Alerta guardar(Alerta alerta) {
        alerta.setIdAlerta(siguiente++);
        if (alerta.getActiva() == null) {
            alerta.setActiva(true);
        }
        alerta.setFechaGeneracion(LocalDateTime.now());
        datos.put(alerta.getIdAlerta(), alerta);
        return alerta;
    }

    @Override
    public boolean guardarSiNoExiste(Alerta alerta) {
        boolean existe = datos.values().stream().anyMatch(a ->
                Boolean.TRUE.equals(a.getActiva())
                        && a.getIdIngrediente().equals(alerta.getIdIngrediente())
                        && a.getTipoAlerta().equals(alerta.getTipoAlerta()));
        if (existe) {
            return false;
        }
        guardar(alerta);
        return true;
    }

    @Override
    public Optional<Alerta> buscarPorId(Long id) {
        return Optional.ofNullable(datos.get(id));
    }

    @Override
    public List<Alerta> listarTodas() {
        return new ArrayList<>(datos.values());
    }

    @Override
    public List<Alerta> listarActivas() {
        return datos.values().stream()
                .filter(a -> Boolean.TRUE.equals(a.getActiva()))
                .toList();
    }

    @Override
    public List<Alerta> listarPorIngrediente(Long idIngrediente) {
        return datos.values().stream()
                .filter(a -> a.getIdIngrediente().equals(idIngrediente))
                .toList();
    }

    @Override
    public void resolver(Long idAlerta) {
        Alerta alerta = datos.get(idAlerta);
        if (alerta != null) {
            alerta.setActiva(false);
            alerta.setFechaResolucion(LocalDateTime.now());
        }
    }
}