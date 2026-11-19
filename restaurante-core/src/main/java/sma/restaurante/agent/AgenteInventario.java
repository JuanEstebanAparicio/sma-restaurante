package sma.restaurante.agent;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;

import sma.restaurante.alerts.AlertaService;
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

public class AgenteInventario extends Agent {

    private InventarioService inventarioService;

    private AlertaService alertaService;

    private PrediccionService prediccionService;

    private AlertaRepository alertaRepository;

    @Override
    protected void setup() {

        System.out.println(
                getLocalName()
                        + " iniciado correctamente."
        );

        IngredienteRepository ingredienteRepository =
                new IngredienteRepositoryPostgres();

        LoteRepository loteRepository =
                new LoteRepositoryPostgres();

        MovimientoRepository movimientoRepository =
                new MovimientoRepositoryPostgres();

        alertaRepository =
                new AlertaRepositoryPostgres();

        inventarioService =
                new InventarioService(
                        ingredienteRepository,
                        loteRepository,
                        movimientoRepository
                );

        alertaService =
                new AlertaService(
                        ingredienteRepository,
                        loteRepository
                );

        prediccionService =
                new PrediccionService(
                        loteRepository,
                        movimientoRepository
                );

        iniciarMonitoreoInventario();
    }

    private void iniciarMonitoreoInventario() {

        addBehaviour(

                new TickerBehaviour(
                        this,
                        30000
                ) {

                    @Override
                    protected void onTick() {

                        System.out.println();
                        System.out.println(
                                "===== CICLO INVENTARIO ====="
                        );

                        procesarAgotados();

                        procesarBajoStock();

                        procesarProximosAVencer();

                        procesarPredicciones();
                    }
                }
        );
    }

    private void procesarAgotados() {

        List<Alerta> alertas =

                alertaService
                        .detectarAgotados();

        alertas.forEach(alerta -> {

            alertaRepository
                    .guardar(alerta);

            System.out.println(
                    "[AGOTADO] "
                            + alerta.getMensaje()
            );
        });
    }

    private void procesarBajoStock() {

        List<Alerta> alertas =

                alertaService
                        .detectarBajoStock();

        alertas.forEach(alerta -> {

            alertaRepository
                    .guardar(alerta);

            System.out.println(
                    "[BAJO STOCK] "
                            + alerta.getMensaje()
            );
        });
    }

    private void procesarProximosAVencer() {

        List<Alerta> alertas =

                alertaService
                        .detectarProximosAVencer();

        alertas.forEach(alerta -> {

            alertaRepository
                    .guardar(alerta);

            System.out.println(
                    "[POR VENCER] "
                            + alerta.getMensaje()
            );
        });
    }

    private void procesarPredicciones() {

        for (Ingrediente ingrediente :

                inventarioService
                        .listarIngredientes()) {

            try {

                String resultado =

                        prediccionService
                                .predecirAgotamiento(
                                        ingrediente.getIdIngrediente(),
                                        ingrediente.getNombre(),
                                        ingrediente.getUnidadMedida()
                                );

                System.out.println();
                System.out.println(resultado);

            } catch (Exception e) {

                System.out.println(
                        "Error procesando predicción de "
                                + ingrediente.getNombre()
                );
            }
        }
    }

    @Override
    protected void takeDown() {

        System.out.println(
                getLocalName()
                        + " finalizado."
        );
    }
}