package sma.restaurante.agent;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;

import sma.restaurante.alerts.AlertaService;
import sma.restaurante.datasource.DatabaseConnection;
import sma.restaurante.prediction.PrediccionService;
import sma.restaurante.repository.AlertaRepository;
import sma.restaurante.repository.IngredienteRepository;
import sma.restaurante.repository.LoteRepository;
import sma.restaurante.repository.MovimientoRepository;
import sma.restaurante.repository.impl.AlertaRepositoryPostgres;
import sma.restaurante.repository.impl.IngredienteRepositoryPostgres;
import sma.restaurante.repository.impl.LoteRepositoryPostgres;
import sma.restaurante.repository.impl.MovimientoRepositoryPostgres;
import sma.restaurante.service.InventarioService;

import java.time.Clock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AgenteInventario extends Agent {

    private static final Logger LOG = Logger.getLogger(AgenteInventario.class.getName());
    private static final long PERIODO_MONITOREO_MS = 30_000;

    /** Lo usaran los mensajes ACL del paso 3 (reservas, disponibilidad de platos). */
    private InventarioService inventarioService;
    private AlertaService alertaService;

    @Override
    protected void setup() {

        IngredienteRepository ingredienteRepository = new IngredienteRepositoryPostgres();
        LoteRepository loteRepository = new LoteRepositoryPostgres();
        MovimientoRepository movimientoRepository = new MovimientoRepositoryPostgres();
        AlertaRepository alertaRepository = new AlertaRepositoryPostgres();

        inventarioService = new InventarioService(
                ingredienteRepository, loteRepository, movimientoRepository);

        alertaService = new AlertaService(
                ingredienteRepository,
                loteRepository,
                alertaRepository,
                new PrediccionService(movimientoRepository),
                Clock.systemDefaultZone());

        addBehaviour(new TickerBehaviour(this, PERIODO_MONITOREO_MS) {
            @Override
            protected void onTick() {
                ejecutarCicloMonitoreo();
            }
        });

        LOG.info(getLocalName() + " iniciado");
    }

    /** Un fallo (por ejemplo, la base de datos pausada) no debe matar al agente. */
    private void ejecutarCicloMonitoreo() {
        try {
            AlertaService.ResultadoSincronizacion resultado = alertaService.sincronizar();

            resultado.nuevas().forEach(alerta ->
                    LOG.warning("[" + alerta.getTipoAlerta() + "] " + alerta.getMensaje()));

            if (resultado.resueltas() > 0) {
                LOG.info(resultado.resueltas()
                        + " alerta(s) resuelta(s) porque la condicion desaparecio");
            }

        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE,
                    "Fallo en el ciclo de monitoreo; se reintenta en el siguiente", e);
        }
    }

    @Override
    protected void takeDown() {
        DatabaseConnection.cerrar();
        LOG.info(getLocalName() + " finalizado");
    }
}