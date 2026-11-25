
package sma.restaurante.agent;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import sma.restaurante.service.ReservaService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private final ReservaService reservaService;

    public AtenderSolicitudesACL(
            Agent agente,
            ReservaService reservaService) {

        super(agente);

        this.reservaService = reservaService;

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

        // Preparar respuesta asociada a la solicitud original.
        ACLMessage respuesta = crearRespuesta(solicitud);

        // Validar ontología.
        if (!ONTOLOGIA.equals(solicitud.getOntology())) {

            enviarRespuesta(
                    respuesta,
                    ACLMessage.NOT_UNDERSTOOD,
                    "(ontologia_no_soportada)"
            );

            return;
        }

        // Validar emisor.
        if (solicitud.getSender() == null) {

            enviarRespuesta(
                    respuesta,
                    ACLMessage.NOT_UNDERSTOOD,
                    "(emisor_no_identificado)"
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

        Matcher matcher =
                PATRON_RESERVAR.matcher(contenido.trim());

        if (!matcher.matches()) {

            enviarRespuesta(
                    respuesta,
                    ACLMessage.NOT_UNDERSTOOD,
                    "(formato_no_reconocido)"
            );

            LOG.warning(
                    "Contenido ACL no reconocido: " + contenido
            );

            return;
        }

        // Obtener identificador del pedido.
        Long idPedido;

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

        LOG.info(
                "Solicitud de reserva recibida. Pedido: "
                        + idPedido
                        + ". Emisor: "
                        + solicitud.getSender().getLocalName()
        );

        // 1. Informar que la solicitud será procesada.
        enviarRespuesta(
                crearRespuesta(solicitud),
                ACLMessage.AGREE,
                "(procesando_reserva " + idPedido + ")"
        );

        // 2. Ejecutar la reserva real en PostgreSQL.
        try {

            ReservaService.ResultadoReserva resultado =
                    reservaService.reservarPedido(idPedido);

            if (resultado.exito()) {

                // 3A. Reserva exitosa.
                enviarRespuesta(
                        crearRespuesta(solicitud),
                        ACLMessage.INFORM,
                        "(reserva_exitosa "
                                + idPedido
                                + " "
                                + resultado.estado()
                                + ")"
                );

                LOG.info(
                        "Reserva completada para el pedido "
                                + idPedido
                );

            } else {

                // 3B. No hay disponibilidad suficiente.
                String faltantes =
                        resultado.ingredientesFaltantesJson();

                if (faltantes == null) {
                    faltantes = "[]";
                }

                enviarRespuesta(
                        crearRespuesta(solicitud),
                        ACLMessage.REFUSE,
                        "(sin_existencia "
                                + idPedido
                                + ") faltantes="
                                + faltantes
                );

                LOG.warning(
                        "Reserva rechazada para el pedido "
                                + idPedido
                                + ". Motivo: "
                                + resultado.mensaje()
                );
            }

        } catch (RuntimeException e) {

            // 3C. Error técnico durante la operación.
            LOG.log(
                    Level.SEVERE,
                    "Error al reservar ingredientes del pedido "
                            + idPedido,
                    e
            );

            enviarRespuesta(
                    crearRespuesta(solicitud),
                    ACLMessage.FAILURE,
                    "(error_reserva " + idPedido + ")"
            );
        }
    }

    private ACLMessage crearRespuesta(ACLMessage solicitud) {

        ACLMessage respuesta = solicitud.createReply();

        respuesta.setProtocol(PROTOCOLO);
        respuesta.setOntology(ONTOLOGIA);

        return respuesta;
    }

    private void enviarRespuesta(
            ACLMessage respuesta,
            int performativa,
            String contenido) {

        respuesta.setPerformative(performativa);
        respuesta.setContent(contenido);

        myAgent.send(respuesta);
    }
}
