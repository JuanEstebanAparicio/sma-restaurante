package sma.restaurante.agent;

import jade.core.Agent;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;

import sma.restaurante.service.CocinaService;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AtenderComandasACL extends AchieveREResponder {

    private static final Logger LOG =
            Logger.getLogger(AtenderComandasACL.class.getName());

    // Acepta: (preparar pedido_101 pizza_queso ...)
    private static final Pattern PATRON_PREPARAR =
            Pattern.compile("^\\(\\s*preparar\\s+pedido_(\\d+)(?:\\s+.*)?\\)$", Pattern.CASE_INSENSITIVE);

    private final CocinaService cocinaService;

    public AtenderComandasACL(Agent agente, CocinaService cocinaService) {
        super(agente, MessageTemplate.and(
                AchieveREResponder.createMessageTemplate(FIPANames.InteractionProtocol.FIPA_REQUEST),
                MessageTemplate.MatchOntology("Restaurante")
        ));
        this.cocinaService = cocinaService;
    }

    @Override
    protected ACLMessage handleRequest(ACLMessage request)
            throws NotUnderstoodException, RefuseException {

        String contenido = request.getContent() == null ? "" : request.getContent().trim();
        Matcher matcher = PATRON_PREPARAR.matcher(contenido);

        if (!matcher.matches()) {
            throw new NotUnderstoodException("(formato_invalido " + contenido + ")");
        }

        long idPedido = Long.parseLong(matcher.group(1));
        Optional<String> motivoRechazo = cocinaService.validarAdmision(idPedido);

        if (motivoRechazo.isPresent()) {
            LOG.warning("Comanda rechazada para pedido_" + idPedido + ": " + motivoRechazo.get());
            throw new RefuseException("(rechazo_comanda pedido_" + idPedido + " " + motivoRechazo.get() + ")");
        }

        // Paso 1 del diagrama AUML: AGREE repitiendo la acción solicitada
        ACLMessage agree = request.createReply();
        agree.setPerformative(ACLMessage.AGREE);
        agree.setContent(contenido);

        LOG.info("AGREE enviado para pedido_" + idPedido);
        return agree;
    }

    @Override
    protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response)
            throws FailureException {

        Matcher matcher = PATRON_PREPARAR.matcher(request.getContent().trim());
        matcher.matches();
        long idPedido = Long.parseLong(matcher.group(1));

        try {
            // Paso 2 del diagrama AUML: planificación interna KDS y cambio a EN_PREPARACION
            cocinaService.iniciarPreparacion(idPedido);

            // Paso 3 del diagrama AUML: INFORM-DONE -> (preparacion_pedido_iniciada pedido_101)
            ACLMessage informDone = request.createReply();
            informDone.setPerformative(ACLMessage.INFORM);
            informDone.setContent("(preparacion_pedido_iniciada pedido_" + idPedido + ")");

            LOG.info("INFORM-DONE enviado: " + informDone.getContent());
            return informDone;

        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Error iniciando preparación del pedido_" + idPedido, e);
            throw new FailureException("(error_cocina pedido_" + idPedido + ")");
        }
    }
}