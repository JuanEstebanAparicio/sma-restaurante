package pedidos.Agente;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import pedidos.DataAccessObject.PedidoDAO;
import pedidos.model.PedidoPendiente;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AgentePedidos extends Agent {

    private static final String ONTOLOGIA = "Restaurante";
    private static final long TIMEOUT_INVENTARIO_MS = 15000L;

    private PedidoDAO pedidoDAO;
    private final Map<String, PedidoPendiente> pedidosEnEsperaInventario = new ConcurrentHashMap<>();

    @Override
    protected void setup() {
        pedidoDAO = new PedidoDAO();
        System.out.println("=================================================================");
        System.out.println("[" + getLocalName() + "] Iniciado. Monitoreando pedidos en BD...");
        System.out.println("=================================================================");

        // 1. Consulta periódica a la BD cada 4 segundos
        addBehaviour(new TickerBehaviour(this, 4000) {
            @Override
            protected void onTick() {
                revisarTimeouts();

                List<PedidoPendiente> nuevosPedidos = pedidoDAO.obtenerPedidosPendientes();
                for (PedidoPendiente pedido : nuevosPedidos) {
                    String conversationId = "REQ_RESERVA_" + pedido.getId();

                    pedidoDAO.actualizarEstado(pedido.getId(), "VALIDANDO");
                    pedidosEnEsperaInventario.put(conversationId, pedido);

                    System.out.println("\n[" + getLocalName() + "] Nuevo pedido detectado en BD: "
                            + pedido.resumenLegible());

                    enviarSolicitudReserva(pedido, conversationId);
                }
            }
        });

        // 2. Escucha AGREE, INFORM (INFORM-DONE) y REFUSE desde AgenteInventario
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                MessageTemplate plantilla = MessageTemplate.and(
                        MessageTemplate.MatchOntology(ONTOLOGIA),
                        MessageTemplate.or(
                                MessageTemplate.MatchPerformative(ACLMessage.AGREE),
                                MessageTemplate.or(
                                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                        MessageTemplate.MatchPerformative(ACLMessage.REFUSE)
                                )
                        )
                );

                ACLMessage msg = myAgent.receive(plantilla);
                if (msg != null) {
                    procesarRespuestaInventario(msg);
                } else {
                    block();
                }
            }
        });
    }

    private void enviarSolicitudReserva(PedidoPendiente pedido, String conversationId) {
        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        request.addReceiver(new AID("AgenteInventario", AID.ISLOCALNAME));
        request.setOntology(ONTOLOGIA);
        request.setProtocol("FIPA-Request");
        request.setConversationId(conversationId);
        request.setContent(pedido.serializarSolicitudReserva());

        send(request);
        System.out.println("[" + getLocalName() + "] -> REQUEST enviado a AgenteInventario: " + request.getContent());
    }

    private void procesarRespuestaInventario(ACLMessage msg) {
        String conversationId = msg.getConversationId();
        if (conversationId == null) {
            return;
        }

        PedidoPendiente pedido = pedidosEnEsperaInventario.get(conversationId);
        if (pedido == null) {
            return;
        }

        switch (msg.getPerformative()) {
            case ACLMessage.AGREE:
                System.out.println("[" + getLocalName() + "] <- AGREE recibido de "
                        + msg.getSender().getLocalName() + ": " + msg.getContent());
                pedidoDAO.actualizarEstado(pedido.getId(), "RESERVANDO");
                break;

            case ACLMessage.INFORM:
                pedidosEnEsperaInventario.remove(conversationId);
                System.out.println("[" + getLocalName() + "] <- INFORM-DONE recibido de "
                        + msg.getSender().getLocalName() + ": " + msg.getContent());

                pedidoDAO.actualizarEstado(pedido.getId(), "SERVIDO");
                servicioCompleto(pedido.getMesa());
                notificarConsumosCuentaPago(pedido);
                break;

            case ACLMessage.REFUSE:
                pedidosEnEsperaInventario.remove(conversationId);
                System.out.println("[" + getLocalName() + "] <- REFUSE recibido de "
                        + msg.getSender().getLocalName() + ": " + msg.getContent());

                pedidoDAO.actualizarEstado(pedido.getId(), "RECHAZADO_SIN_STOCK");
                break;
        }
    }

    private void servicioCompleto(String mesa) {
        System.out.println("[" + getLocalName() + "] Ejecutando servicioCompleto(" + mesa + ")");
    }

    private void notificarConsumosCuentaPago(PedidoPendiente pedido) {
        ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
        inform.addReceiver(new AID("AgenteCuentaPago", AID.ISLOCALNAME));
        inform.setOntology(ONTOLOGIA);
        inform.setProtocol("FIPA-Inform");
        inform.setContent("(consumos " + pedido.getMesa() + " pedido_" + pedido.getId() + ")");

        send(inform);
        System.out.println("[" + getLocalName() + "] -> INFORM enviado a AgenteCuentaPago: " + inform.getContent());
    }

    private void revisarTimeouts() {
        long ahora = System.currentTimeMillis();
        pedidosEnEsperaInventario.entrySet().removeIf(entry -> {
            PedidoPendiente pedido = entry.getValue();
            if (ahora - pedido.getTimestampEnvio() > TIMEOUT_INVENTARIO_MS) {
                System.err.println("[" + getLocalName() + "] Sin respuesta final de AgenteInventario para pedido #"
                        + pedido.getId() + ". Devolviendo estado a PENDIENTE.");
                pedidoDAO.actualizarEstado(pedido.getId(), "PENDIENTE");
                return true;
            }
            return false;
        });
    }
}