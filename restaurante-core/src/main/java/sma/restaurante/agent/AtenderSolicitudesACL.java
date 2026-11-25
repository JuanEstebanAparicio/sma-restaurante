
package sma.restaurante.agent;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.FIPANames;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

public class AtenderSolicitudesACL extends CyclicBehaviour {

    private static final Logger LOG =
            Logger.getLogger(AtenderSolicitudesACL.class.getName());

    private static final String ONTOLOGIA = "Restaurante";

    private static final String PROTOCOLO =
            FIPANames.InteractionProtocol.FIPA_REQUEST;

    private static final Pattern PATRON_RESERVAR = Pattern.compile(
            "^\\(\\s*reservar\\s+(?:pedido_)?(\\d+)"
                    + "(?:\\s+([a-zA-Z0-9_-]+))?\\s*\\)$",
            Pattern.CASE_INSENSITIVE
    );

    private final MessageTemplate plantilla;

    public AtenderSolicitudesACL(Agent agente) {

        super(agente);

        plantilla = MessageTemplate.and(
                MessageTemplate.MatchProtocol(PROTOCOLO),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST)
        );
    }

    @Override
    public void action() {

        ACLMessage solicitud = myAgent.receive(plantilla);

        if (solicitud == null) {
            block();
            return;
        }

        procesarSolicitud(solicitud);
    }

    private void procesarSolicitud(ACLMessage solicitud) {

        // 1. Validar ontología.
        if (!ONTOLOGIA.equals(solicitud.getOntology())) {

            responder(
                    solicitud,
                    ACLMessage.NOT_UNDERSTOOD,
                    "(ontologia_no_soportada)"
            );

            return;
        }

        // 2. Validar emisor.
        if (solicitud.getSender() == null) {

            responder(
                    solicitud,
                    ACLMessage.NOT_UNDERSTOOD,
                    "(emisor_no_identificado)"
            );

            return;
        }

        // 3. Validar contenido.
        String contenido = solicitud.getContent();

        if (contenido == null || contenido.trim().isEmpty()) {

            responder(
                    solicitud,
                    ACLMessage.NOT_UNDERSTOOD,
                    "(contenido_vacio)"
            );

            return;
        }

        Matcher matcher = PATRON_RESERVAR.matcher(contenido.trim());

        if (!matcher.matches()) {

            responder(
                    solicitud,
                    ACLMessage.NOT_UNDERSTOOD,
                    "(formato_no_reconocido)"
            );

            LOG.warning("Contenido ACL no reconocido: " + contenido);

            return;
        }

        // 4. Obtener identificador del pedido.
        Long idPedido;

        try {

            idPedido = Long.parseLong(matcher.group(1));

            if (idPedido <= 0) {
                throw new NumberFormatException();
            }

        } catch (NumberFormatException e) {

            responder(
                    solicitud,
                    ACLMessage.NOT_UNDERSTOOD,
                    "(id_pedido_invalido)"
            );

            return;
        }

        String producto = matcher.group(2);

        LOG.info(
                "Solicitud ACL recibida. Pedido: "
                        + idPedido
                        + ". Producto: "
                        + (producto != null ? producto : "no especificado")
                        + ". Emisor: "
                        + solicitud.getSender().getLocalName()
        );

        // 5. Confirmar que la solicitud será procesada.
        responder(
                solicitud,
                ACLMessage.AGREE,
                "(procesando_reserva " + idPedido + ")"
        );

        // 6. Simular el procesamiento de la solicitud.
        simularReserva(solicitud, idPedido);
    }

    private void simularReserva(
            ACLMessage solicitud,
            Long idPedido) {

        // CASO 1: Pedido 101.
        // Simula una reserva exitosa.
        if (idPedido == 101L) {

            responder(
                    solicitud,
                    ACLMessage.INFORM,
                    "(reserva_exitosa " + idPedido + ")"
            );

            LOG.info(
                    "Simulación exitosa: reserva aceptada para el pedido "
                            + idPedido
            );

            return;
        }

        // CASO 2: Pedido 102.
        // Simula que no existen ingredientes suficientes.
        if (idPedido == 102L) {

            responder(
                    solicitud,
                    ACLMessage.REFUSE,
                    "(sin_existencia " + idPedido + ")"
            );

            LOG.warning(
                    "Simulación: reserva rechazada por falta de existencias. "
                            + "Pedido: " + idPedido
            );

            return;
        }

        // CASO 3: Cualquier otro pedido.
        // No está contemplado en los datos simulados.
        responder(
                solicitud,
                ACLMessage.REFUSE,
                "(pedido_no_simulado " + idPedido + ")"
        );

        LOG.warning(
                "Pedido no contemplado en la simulación: " + idPedido
        );
    }

    private void responder(
            ACLMessage solicitud,
            int performativa,
            String contenido) {

        ACLMessage respuesta = solicitud.createReply();

        respuesta.setProtocol(PROTOCOLO);
        respuesta.setOntology(ONTOLOGIA);
        respuesta.setPerformative(performativa);
        respuesta.setContent(contenido);

        myAgent.send(respuesta);

        LOG.info(
                "Respuesta ACL enviada: "
                        + ACLMessage.getPerformative(performativa)
                        + " | Contenido: "
                        + contenido
        );
    }
}
