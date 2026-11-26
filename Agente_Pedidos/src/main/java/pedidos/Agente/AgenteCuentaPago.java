package pedidos.Agente;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import pedidos.DataAccessObject.CuentaPagoDAO;
import pedidos.model.CuentaPedido;

import java.util.Optional;
import java.util.logging.Logger;

public class AgenteCuentaPago extends Agent {

    private static final Logger LOG = Logger.getLogger(AgenteCuentaPago.class.getName());
    private static final String ONTOLOGIA = "Restaurante";

    private CuentaPagoDAO cuentaPagoDAO;

    @Override
    protected void setup() {
        cuentaPagoDAO = new CuentaPagoDAO();
        LOG.info(getLocalName() + " iniciado y escuchando consumos y solicitudes de pago.");

        MessageTemplate plantillaConsumos = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchOntology(ONTOLOGIA)
        );

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = myAgent.receive(plantillaConsumos);
                if (msg != null) {
                    procesarNotificacionConsumos(msg);
                } else {
                    block();
                }
            }
        });

        MessageTemplate plantillaPagos = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                        MessageTemplate.MatchOntology(ONTOLOGIA)
                )
        );

        addBehaviour(new AchieveREResponder(this, plantillaPagos) {
            @Override
            protected ACLMessage handleRequest(ACLMessage request) throws NotUnderstoodException, RefuseException {
                String[] tokens = parsearTokens(request.getContent());
                if (tokens.length < 2 || !"pagar".equalsIgnoreCase(tokens[0])) {
                    throw new NotUnderstoodException("(formato_no_reconocido)");
                }

                long idPedido = extraerIdPedido(tokens[1]);
                if (idPedido <= 0) {
                    throw new NotUnderstoodException("(pedido_invalido)");
                }

                ACLMessage agree = request.createReply();
                agree.setPerformative(ACLMessage.AGREE);
                agree.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
                agree.setOntology(ONTOLOGIA);
                agree.setContent("(procesando_pago pedido_" + idPedido + ")");
                return agree;
            }

            @Override
            protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
                String[] tokens = parsearTokens(request.getContent());
                long idPedido = extraerIdPedido(tokens[1]);
                String metodo = (tokens.length >= 3) ? tokens[2] : "EFECTIVO";

                Optional<Double> pagoConfirmado = cuentaPagoDAO.procesarPago(idPedido, metodo);
                if (pagoConfirmado.isEmpty()) {
                    throw new FailureException("(cuenta_no_disponible pedido_" + idPedido + ")");
                }

                System.out.println("[" + getLocalName() + "] Pago confirmado para pedido_" + idPedido
                        + " | Método: " + metodo + " | Total: $" + pagoConfirmado.get());

                ACLMessage inform = request.createReply();
                inform.setPerformative(ACLMessage.INFORM);
                inform.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
                inform.setOntology(ONTOLOGIA);
                inform.setContent("(pago_confirmado pedido_" + idPedido + " " + pagoConfirmado.get() + ")");
                return inform;
            }
        });
    }

    private void procesarNotificacionConsumos(ACLMessage msg) {
        String[] tokens = parsearTokens(msg.getContent());
        if (tokens.length < 3 || !"consumos".equalsIgnoreCase(tokens[0])) {
            return;
        }

        String mesa = tokens[1];
        long idPedido = extraerIdPedido(tokens[2]);
        if (idPedido <= 0) {
            return;
        }

        System.out.println("[" + getLocalName() + "] <- INFORM recibido de "
                + msg.getSender().getLocalName() + ": " + msg.getContent());

        Optional<CuentaPedido> cuenta = cuentaPagoDAO.registrarConsumosYGenerarCuenta(idPedido, mesa);
        cuenta.ifPresentOrElse(
                c -> System.out.println("[" + getLocalName() + "] Factura generada en BD: " + c.resumenFactura()),
                () -> System.err.println("[" + getLocalName() + "] No se encontraron detalles para generar cuenta del pedido #" + idPedido)
        );
    }

    private String[] parsearTokens(String contenido) {
        if (contenido == null) {
            return new String[0];
        }
        String limpio = contenido.replace("(", "").replace(")", "").trim();
        return limpio.isEmpty() ? new String[0] : limpio.split("\\s+");
    }

    private long extraerIdPedido(String tokenPedido) {
        try {
            return Long.parseLong(tokenPedido.replace("pedido_", "").trim());
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}