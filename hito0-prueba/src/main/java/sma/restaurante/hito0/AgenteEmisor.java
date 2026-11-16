package sma.restaurante.hito0;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;

/**
 * Hito 0: hace de "Pedidos".
 * Envía un REQUEST cada 10 segundos y muestra cualquier respuesta que reciba.
 */
public class AgenteEmisor extends Agent {

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " listo");

        // Comportamiento periódico: onTick() se ejecuta cada 10 segundos,
        // tiempo suficiente para activar el Sniffer y ver el intercambio
        addBehaviour(new TickerBehaviour(this, 10000) {
            @Override
            protected void onTick() {
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.addReceiver(new AID("receptor", AID.ISLOCALNAME));      // :receiver
                msg.setOntology("Restaurante");                              // :ontology
                msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST); // :protocol
                msg.setConversationId("reserva-" + System.currentTimeMillis()); // :conversation-id
                msg.setContent("(reservar pedido_101 pizza_queso)");         // :content
                myAgent.send(msg);
                System.out.println(getLocalName() + ": envié REQUEST -> " + msg.getContent());
            }
        });

        // Comportamiento cíclico: escucha cualquier respuesta (AGREE, FAILURE, ...)
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                // Sin plantilla: recibe todo lo que llegue
                ACLMessage respuesta = myAgent.receive();

                if (respuesta != null) {
                    System.out.println(getLocalName() + ": recibí "
                            + ACLMessage.getPerformative(respuesta.getPerformative())
                            + " de " + respuesta.getSender().getLocalName()
                            + " [" + respuesta.getConversationId() + "] -> " + respuesta.getContent());
                } else {
                    block();
                }
            }
        });
    }
}