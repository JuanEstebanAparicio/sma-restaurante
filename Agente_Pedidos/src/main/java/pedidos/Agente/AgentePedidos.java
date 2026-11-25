package pedidos.Agente;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
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
        System.out.println("[" + getLocalName() + "] Iniciado. Monitoreando pedidos RECIBIDO en BD...");
        System.out.println("=================================================================");

        addBehaviour(new TickerBehaviour(this, 4000) {
            @Override
            protected void onTick() {
                revisarTimeouts();

                List<PedidoPendiente> nuevosPedidos = pedidoDAO.obtenerPedidosRecibidos();
                for (PedidoPendiente pedido : nuevosPedidos) {
                    String conversationId = "REQ_RESERVA_" + pedido.getIdPedido();

                    pedidoDAO.registrarSnapshotYValidar(pedido);
                    pedidosEnEsperaInventario.put(conversationId, pedido);

                    System.out.println("\n[" + getLocalName() + "] Nuevo pedido detectado en BD: "
                            + pedido.resumenLegible());

                    enviarSolicitudReserva(pedido, conversationId);
                }
            }
        });

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                MessageTemplate plantilla = MessageTemplate.and(
                        MessageTemplate.MatchOntology(ONTOLOGIA),
                        MessageTemplate.or(
                                MessageTemplate.MatchPerformative(ACLMessage.AGREE),
                                MessageTemplate.or(
                                        MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                        MessageTemplate.or(
                                                MessageTemplate.MatchPerformative(ACLMessage.REFUSE),
                                                MessageTemplate.or(
                                                        MessageTemplate.MatchPerformative(ACLMessage.NOT_UNDERSTOOD),
                                                        MessageTemplate.MatchPerformative(ACLMessage.FAILURE)
                                                )
                                        )
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
        request.addReceiver(new AID("inventario", AID.ISLOCALNAME));
        request.setOntology(ONTOLOGIA);
        request.setProtocol("FIPA-Request");
        request.setConversationId(conversationId);
        request.setContent(pedido.serializarSolicitudReserva());

        send(request);
        System.out.println("[" + getLocalName() + "] -> REQUEST enviado a inventario: " + request.getContent());
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
                break;

            case ACLMessage.INFORM:
                pedidosEnEsperaInventario.remove(conversationId);
                System.out.println("[" + getLocalName() + "] <- INFORM recibido de "
                        + msg.getSender().getLocalName() + ": " + msg.getContent());

                servicioCompleto(pedido.getMesa());
                notificarConsumosCuentaPago(pedido);
                pedidoDAO.actualizarEstado(pedido.getIdPedido(), "COMPLETADO");
                break;

            case ACLMessage.REFUSE:
                pedidosEnEsperaInventario.remove(conversationId);
                System.out.println("[" + getLocalName() + "] <- REFUSE recibido de "
                        + msg.getSender().getLocalName() + ": " + msg.getContent());

                pedidoDAO.actualizarEstado(pedido.getIdPedido(), "RECHAZADO");
                break;

            case ACLMessage.NOT_UNDERSTOOD:
            case ACLMessage.FAILURE:
                pedidosEnEsperaInventario.remove(conversationId);
                System.err.println("[" + getLocalName() + "] <- "
                        + ACLMessage.getPerformative(msg.getPerformative()) + " recibido de "
                        + msg.getSender().getLocalName() + ": " + msg.getContent());

                pedidoDAO.actualizarEstado(pedido.getIdPedido(), "RECHAZADO");
                break;
        }
    }

    private void servicioCompleto(String mesa) {
        System.out.println("[" + getLocalName() + "] Ejecutando servicioCompleto(" + mesa + ")");
    }

    private void notificarConsumosCuentaPago(PedidoPendiente pedido) {
        String contenido = "(consumos " + pedido.getMesa() + " pedido_" + pedido.getIdPedido() + ")";

        if (!existeAgenteEnPlataforma("AgenteCuentaPago")) {
            System.out.println("[" + getLocalName() + "] -> AgenteCuentaPago aún no está activo en JADE. "
                    + "Consumo registrado para facturación: " + contenido);
            return;
        }

        ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
        inform.addReceiver(new AID("AgenteCuentaPago", AID.ISLOCALNAME));
        inform.setOntology(ONTOLOGIA);
        inform.setProtocol("FIPA-Inform");
        inform.setContent(contenido);

        send(inform);
        System.out.println("[" + getLocalName() + "] -> INFORM enviado a AgenteCuentaPago: " + inform.getContent());
    }

    private boolean existeAgenteEnPlataforma(String nombreLocal) {
        try {
            AMSAgentDescription plantilla = new AMSAgentDescription();
            plantilla.setName(new AID(nombreLocal, AID.ISLOCALNAME));
            SearchConstraints restricciones = new SearchConstraints();
            restricciones.setMaxResults(1L);

            AMSAgentDescription[] resultados = AMSService.search(this, plantilla, restricciones);
            return resultados != null && resultados.length > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void revisarTimeouts() {
        long ahora = System.currentTimeMillis();
        pedidosEnEsperaInventario.entrySet().removeIf(entry -> {
            PedidoPendiente pedido = entry.getValue();
            if (ahora - pedido.getTimestampEnvio() > TIMEOUT_INVENTARIO_MS) {
                System.err.println("[" + getLocalName() + "] Sin respuesta final de inventario para pedido #"
                        + pedido.getIdPedido() + ". Devolviendo estado a RECIBIDO.");
                pedidoDAO.actualizarEstado(pedido.getIdPedido(), "RECIBIDO");
                return true;
            }
            return false;
        });
    }
}