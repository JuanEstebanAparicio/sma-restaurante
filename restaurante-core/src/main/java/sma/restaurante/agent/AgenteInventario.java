package sma.restaurante.agent;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;

import sma.restaurante.alerts.AlertaService;
import sma.restaurante.datasource.DatabaseConnection;
import sma.restaurante.domain.Alerta;
import sma.restaurante.domain.Ingrediente;
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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AgenteInventario extends Agent {

    private static final Logger LOG = Logger.getLogger(AgenteInventario.class.getName());
    private static final long PERIODO_MONITOREO_MS = 30_000;

    private InventarioService inventarioService;
    private AlertaService alertaService;
    private PrediccionService prediccionService;
    private AlertaRepository alertaRepository;

    @Override
    protected void setup() {

        IngredienteRepository ingredienteRepository = new IngredienteRepositoryPostgres();
        LoteRepository loteRepository = new LoteRepositoryPostgres();
        MovimientoRepository movimientoRepository = new MovimientoRepositoryPostgres();
        alertaRepository = new AlertaRepositoryPostgres();

        inventarioService = new InventarioService(
                ingredienteRepository, loteRepository, movimientoRepository);
        alertaService = new AlertaService(ingredienteRepository, loteRepository);
        prediccionService = new PrediccionService(loteRepository, movimientoRepository);

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
            publicar("AGOTADO", alertaService.detectarAgotados());
            publicar("BAJO STOCK", alertaService.detectarBajoStock());
            publicar("POR VENCER", alertaService.detectarProximosAVencer());
            procesarPredicciones();
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Fallo en el ciclo de monitoreo; se reintenta en el siguiente", e);
        }
    }

    private void publicar(String etiqueta, List<Alerta> alertas) {
        for (Alerta alerta : alertas) {
            if (alertaRepository.guardarSiNoExiste(alerta)) {
                LOG.warning("[" + etiqueta + "] " + alerta.getMensaje());
            }
        }
    }

    private void procesarPredicciones() {
        for (Ingrediente ingrediente : inventarioService.listarIngredientes()) {
            try {
                String resultado = prediccionService.predecirAgotamiento(
                        ingrediente.getIdIngrediente(),
                        ingrediente.getNombre(),
                        ingrediente.getUnidadMedida());
                LOG.fine(resultado);
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING,
                        "Error procesando la prediccion de " + ingrediente.getNombre(), e);
            }
        }
    }

    @Override
    protected void takeDown() {
        DatabaseConnection.cerrar();
        LOG.info(getLocalName() + " finalizado");
    }
}