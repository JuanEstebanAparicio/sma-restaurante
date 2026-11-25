package sma.restaurante.service;

import sma.restaurante.repository.impl.CocinaRepositoryPostgres;
import sma.restaurante.repository.impl.CocinaRepositoryPostgres.ComandaActiva;
import sma.restaurante.repository.impl.CocinaRepositoryPostgres.PlatoComanda;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CocinaService {

    private static final int CAPACIDAD_MAXIMA_ESTACION = 8;

    public record ResultadoCicloCocina(
            List<Long> pedidosCompletados,
            List<Long> pedidosRetrasados
    ) {}

    private final CocinaRepositoryPostgres repo;

    public CocinaService(CocinaRepositoryPostgres repo) {
        this.repo = repo;
    }

    /**
     * Valida si cocina puede aceptar el pedido (para responder AGREE o REFUSE).
     * Respeta la restricción de no alterar el contenido del pedido ni aceptar pedidos no aprobados.
     */
    public Optional<String> validarAdmision(long idPedido) {
        Optional<String> estadoOpt = repo.obtenerEstadoPedido(idPedido);
        if (estadoOpt.isEmpty()) {
            return Optional.of("pedido_inexistente");
        }
        String estado = estadoOpt.get();
        if (!"ACEPTADO".equals(estado)) {
            return Optional.of("estado_invalido_" + estado.toLowerCase());
        }

        List<PlatoComanda> platos = repo.listarPlatosPedido(idPedido);
        if (platos.isEmpty()) {
            return Optional.of("pedido_sin_platillos");
        }

        for (PlatoComanda p : platos) {
            if (repo.contarCargaEstacion(p.estacion()) >= CAPACIDAD_MAXIMA_ESTACION) {
                return Optional.of("estacion_saturada_" + p.estacion().toLowerCase());
            }
        }
        return Optional.empty();
    }

    /**
     * Ejecuta la planificación KDS:
     * 1. Calcula el tiempo máximo del pedido (T_max).
     * 2. Calcula el retardo de cada plato (T_max - T_i) para sincronizar la entrega.
     * 3. Registra la comanda y cambia el pedido a EN_PREPARACION.
     */
    public void iniciarPreparacion(long idPedido) {
        List<PlatoComanda> platos = repo.listarPlatosPedido(idPedido);
        if (platos.isEmpty()) {
            throw new IllegalStateException("El pedido " + idPedido + " no tiene platillos registrados.");
        }

        int tiempoObjetivoMin = platos.stream()
                .mapToInt(PlatoComanda::tiempoPreparacionMin)
                .max()
                .orElse(10);

        int totalPlatos = platos.stream().mapToInt(PlatoComanda::cantidad).sum();
        String prioridad = totalPlatos >= 6 ? "ALTA" : "NORMAL";

        Instant ahora = Instant.now();
        Instant finEstimada = ahora.plus(Duration.ofMinutes(tiempoObjetivoMin));

        long idComanda = repo.registrarComanda(
                idPedido,
                prioridad,
                tiempoObjetivoMin,
                ahora,
                finEstimada
        );

        for (PlatoComanda plato : platos) {
            int retardoMin = Math.max(0, tiempoObjetivoMin - plato.tiempoPreparacionMin());
            boolean arrancaYa = (retardoMin == 0);

            Instant inicioProgramado = ahora.plus(Duration.ofMinutes(retardoMin));
            Instant inicioReal = arrancaYa ? ahora : null;
            String estadoItem = arrancaYa ? "EN_PREPARACION" : "EN_ESPERA_SINCRONIZACION";

            repo.registrarItemComanda(
                    idComanda,
                    plato,
                    retardoMin,
                    estadoItem,
                    inicioProgramado,
                    inicioReal,
                    finEstimada
            );
        }

        repo.marcarPedidoEnPreparacion(idPedido);
    }

    /**
     * Ciclo híbrido de supervisión KDS:
     * - Activa platos cuya ventana de sincronización ya llegó.
     * - Marca como LISTO los platos cuyo tiempo de cocción finalizó.
     * - Detecta comandas retrasadas o completadas.
     */
    public ResultadoCicloCocina supervisarPreparaciones() {
        repo.avanzarSincronizacionYCompletados();

        List<Long> completados = new ArrayList<>();
        List<Long> retrasados = new ArrayList<>();
        Instant ahora = Instant.now();

        for (ComandaActiva comanda : repo.listarComandasActivas()) {
            int pendientes = repo.contarItemsPendientes(comanda.idComanda());

            if (pendientes == 0) {
                repo.completarComandaYPedido(comanda.idComanda(), comanda.idPedido());
                completados.add(comanda.idPedido());
            } else if ("EN_PREPARACION".equals(comanda.estado())
                    && ahora.isAfter(comanda.fechaFinEstimada())) {
                repo.marcarComandaRetrasada(comanda.idComanda());
                retrasados.add(comanda.idPedido());
            }
        }

        return new ResultadoCicloCocina(completados, retrasados);
    }
}