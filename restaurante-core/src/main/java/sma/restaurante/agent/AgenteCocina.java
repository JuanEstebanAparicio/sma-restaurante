package sma.restaurante.agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;

import sma.restaurante.repository.impl.CocinaRepositoryPostgres;
import sma.restaurante.service.CocinaService;
import sma.restaurante.service.CocinaService.ResultadoCicloCocina;

import java.util.logging.Level;
import java.util.logging.Logger;

public class AgenteCocina extends Agent {

    private static final Logger LOG =
            Logger.getLogger(AgenteCocina.class.getName());

    private static final long PERIODO_SUPERVISION_MS = 10_000;

    private CocinaService cocinaService;

    @Override
    protected void setup() {
        CocinaRepositoryPostgres repo = new CocinaRepositoryPostgres();
        cocinaService = new CocinaService(repo);

        // 1. Componente reactivo: recibe REQUEST de AgentePedidos y responde AGREE + INFORM-DONE
        addBehaviour(new AtenderComandasACL(this, cocinaService));

        // 2. Componente deliberativo/KDS: sincroniza tiempos de inicio, detecta retrasos y notifica pedidos listos
        addBehaviour(new TickerBehaviour(this, PERIODO_SUPERVISION_MS) {
            @Override
            protected void onTick() {
                supervisarCocina();
            }
        });

        LOG.info(getLocalName() + " iniciado correctamente");
    }

    private void supervisarCocina() {
        try {
            ResultadoCicloCocina resultado = cocinaService.supervisarPreparaciones();

            for (Long idPedido : resultado.pedidosRetrasados()) {
                LOG.warning("Retraso detectado en cocina para pedido_" + idPedido);
                notificarAgente("pedidos", "(retraso_cocina pedido_" + idPedido + ")");
            }

            for (Long idPedido : resultado.pedidosCompletados()) {
                LOG.info("Pedido listo en cocina: pedido_" + idPedido);
                notificarAgente("pedidos", "(pedido_listo pedido_" + idPedido + ")");
                notificarAgente("inventario", "(preparacion_completada pedido_" + idPedido + ")");
            }
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Error en supervisión periódica de cocina", e);
        }
    }

    private void notificarAgente(String nombreLocalDestino, String contenido) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID(nombreLocalDestino, AID.ISLOCALNAME));
        msg.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
        msg.setOntology("Restaurante");
        msg.setContent(contenido);
        send(msg);
    }

    @Override
    protected void takeDown() {
        LOG.info(getLocalName() + " finalizado");
    }
}