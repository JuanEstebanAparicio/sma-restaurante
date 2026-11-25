package sma.restaurante.agent;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import sma.restaurante.service.ReservaService;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private final ReservaService reservaService;

    public AtenderSolicitudesACL(Agent agente, ReservaService reservaService) {
        super(agente);
        this.reservaService = Objects.requireNonNull(
                reservaService,
                "ReservaService no puede ser null"
        );
        this.plantilla = MessageTemplate.and(
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
            responder(solicitud, ACLMessage.NOT_UNDERSTOOD, "(ontologia_no_soportada)");
            return;
        }

        // 2. Validar emisor.
        if (solicitud.getSender() == null) {
            responder(solicitud, ACLMessage.NOT_UNDERSTOOD, "(emisor_no_identificado)");
            return;
        }

        // 3. Validar contenido.
        String contenido = solicitud.getContent();
        if (contenido == null || contenido.trim().isEmpty()) {
            responder(solicitud, ACLMessage.NOT_UNDERSTOOD, "(contenido_vacio)");
            return;
        }

        Matcher matcher = PATRON_RESERVAR.matcher(contenido.trim());
        if (!matcher.matches()) {
            responder(solicitud, ACLMessage.NOT_UNDERSTOOD, "(formato_no_reconocido)");
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
            responder(solicitud, ACLMessage.NOT_UNDERSTOOD, "(id_pedido_invalido)");
            return;
        }

        LOG.info(
                "Solicitud ACL recibida. Pedido: " + idPedido
                        + " | Emisor: " + solicitud.getSender().getLocalName()
        );

        // 5. Ejecutar la reserva real en PostgreSQL y responder según el protocolo.
        procesarReserva(solicitud, idPedido);
    }

    private void procesarReserva(ACLMessage solicitud, Long idPedido) {
        try {
            ReservaService.ResultadoReserva resultado =
                    reservaService.reservarPedido(idPedido);

            if (resultado.exito()) {
                responder(
                        solicitud,
                        ACLMessage.INFORM,
                        "(reserva_exitosa " + resultado.idPedido() + ")"
                );
                LOG.info("Reserva exitosa para el pedido " + resultado.idPedido()
                        + ": " + resultado.mensaje());
                if (myAgent instanceof AgenteInventario agenteInventario) {
                    agenteInventario.solicitarRecalculoInmediato();
                }
                return;
            }

            if (!resultado.ingredientesFaltantes().isEmpty()) {
                String faltantes = formatearFaltantesACL(resultado.ingredientesFaltantes());
                String contenido = faltantes.isEmpty()
                        ? "(sin_existencia " + resultado.idPedido() + ")"
                        : "(sin_existencia " + resultado.idPedido() + " " + faltantes + ")";

                responder(solicitud, ACLMessage.REFUSE, contenido);
                LOG.warning("Reserva rechazada por falta de existencias. Pedido: "
                        + resultado.idPedido() + " | Faltantes: " + faltantes);
                return;
            }

            if ("RECHAZADO".equals(resultado.estado())) {
                responder(
                        solicitud,
                        ACLMessage.REFUSE,
                        "(sin_existencia " + resultado.idPedido() + ")"
                );
                LOG.warning("Pedido rechazado: " + resultado.idPedido()
                        + " | Motivo: " + resultado.mensaje());
                return;
            }

            // Pedido inexistente, estado inválido o inconsistencia de reservas previas.
            responder(
                    solicitud,
                    ACLMessage.FAILURE,
                    "(error_reserva " + resultado.idPedido() + ")"
            );
            LOG.warning("No se pudo completar la reserva del pedido "
                    + resultado.idPedido() + ": " + resultado.mensaje());

        } catch (RuntimeException e) {
            responder(
                    solicitud,
                    ACLMessage.FAILURE,
                    "(error_reserva " + idPedido + ")"
            );
            LOG.log(
                    Level.SEVERE,
                    "Error técnico al reservar ingredientes para el pedido " + idPedido,
                    e
            );
        }
    }

    private static String formatearFaltantesACL(List<String> faltantes) {
        return faltantes.stream()
                .map(AtenderSolicitudesACL::normalizarTokenACL)
                .filter(token -> !token.isEmpty())
                .collect(Collectors.joining(" "));
    }

    private static String normalizarTokenACL(String nombre) {
        String sinAcentos = Normalizer.normalize(nombre.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinAcentos
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private void responder(ACLMessage solicitud, int performativa, String contenido) {
        ACLMessage respuesta = solicitud.createReply();

        respuesta.setProtocol(PROTOCOLO);
        respuesta.setOntology(ONTOLOGIA);
        respuesta.setPerformative(performativa);
        respuesta.setContent(contenido);

        myAgent.send(respuesta);

        LOG.info(
                "Respuesta ACL enviada: "
                        + ACLMessage.getPerformative(performativa)
                        + " | Contenido: " + contenido
        );
    }
}