
package sma.restaurante.agent;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

/**
 * Recibe y valida solicitudes ACL del AgentePedidos.
 *
 * Protocolo: FIPA-Request
 * Ontología: Restaurante
 *
 * Formatos aceptados:
 *
 * (reservar 101)
 * (reservar pedido_101)
 * (reservar pedido_101 pizza_queso)
 */
public class AtenderSolicitudesACL extends CyclicBehaviour {

    private static final Logger LOG =
            Logger.getLogger(AtenderSolicitudesACL.class.getName());

    private static final String ONTOLOGIA = "Restaurante";

    private static final String PROTOCOLO = "FIPA-Request";

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

        ACLMessage respuesta = solicitud.createReply();

        respuesta.setProtocol(PROTOCOLO);
        respuesta.setOntology(ONTOLOGIA);

        // Validar ontología.
        if (!ONTOLOGIA.equals(solicitud.getOntology())) {

            enviarRespuesta(
                    respuesta,
                    ACLMessage.NOT_UNDERSTOOD,
                    "(ontologia_no_soportada)"
            );

            LOG.warning(
                    "Solicitud recibida con ontología no soportada: "
                            + solicitud.getOntology()
            );

            return;
        }

        // Validar contenido.
        String contenido = solicitud.getContent();

        if (contenido == null) {

            enviarRespuesta(
                    respuesta,
                    ACLMessage.NOT_UNDERSTOOD,
                    "(contenido_vacio)"
            );

            return;
        }

        Matcher matcher = PATRON_RESERVAR.matcher(contenido.trim());

        if (!matcher.matches()) {

            enviarRespuesta(
                    respuesta,
                    ACLMessage.NOT_UNDERSTOOD,
                    "(formato_no_reconocido)"
            );

            LOG.warning(
                    "Formato ACL no reconocido desde "
                            + obtenerEmisor(solicitud)
                            + ": " + contenido
            );

            return;
        }

        // Extraer identificador del pedido.
        long idPedido;

        try {

            idPedido = Long.parseLong(matcher.group(1));

            if (idPedido <= 0) {
                throw new NumberFormatException();
            }

        } catch (NumberFormatException e) {

            enviarRespuesta(
                    respuesta,
                    ACLMessage.NOT_UNDERSTOOD,
                    "(id_pedido_invalido)"
            );

            return;
        }

        // Nombre del plato opcional.
        String nombrePlato = matcher.group(2);

        LOG.info(
                "Solicitud ACL recibida desde "
                        + obtenerEmisor(solicitud)
                        + ". Pedido: " + idPedido
                        + (nombrePlato != null
                        ? ". Plato: " + nombrePlato
                        : "")
        );

        /*
         * IMPORTANTE:
         *
         * Aquí conectaremos ReservaService.
         *
         * El servicio deberá consultar los ingredientes del pedido
         * en PostgreSQL y ejecutar la reserva transaccional.
         *
         * Todavía no se confirma ninguna reserva.
         */

        enviarRespuesta(
                respuesta,
                ACLMessage.REFUSE,
                "(servicio_reservas_pendiente " + idPedido + ")"
        );
    }

    private void enviarRespuesta(
            ACLMessage respuesta,
            int performativa,
            String contenido) {

        respuesta.setPerformative(performativa);
        respuesta.setContent(contenido);

        myAgent.send(respuesta);
    }

    private String obtenerEmisor(ACLMessage mensaje) {

        if (mensaje.getSender() == null) {
            return "desconocido";
        }

        return mensaje.getSender().getLocalName();
    }
}
