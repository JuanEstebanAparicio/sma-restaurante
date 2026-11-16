package sma.restaurante.hito0;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * Hito 0: hace de "Inventario".
 * Recibe un REQUEST de la ontología "Restaurante" y responde AGREE.
 */
public class AgenteReceptor extends Agent {

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " listo");

        // Comportamiento cíclico: JADE llama a action() una y otra vez
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                // Solo nos interesan los REQUEST de la ontología "Restaurante"
                MessageTemplate plantilla = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                        MessageTemplate.MatchOntology("Restaurante"));

                // receive() NO espera: devuelve el mensaje o null si no hay ninguno
                ACLMessage msg = myAgent.receive(plantilla);

                if (msg != null) {
                    System.out.println(getLocalName() + ": recibí REQUEST de "
                            + msg.getSender().getLocalName()
                            + " [" + msg.getConversationId() + "] -> " + msg.getContent());

                    // createReply() apunta al emisor y copia protocolo, ontología y conversation-id
                    ACLMessage respuesta = msg.createReply();
                    respuesta.setPerformative(ACLMessage.AGREE);
                    respuesta.setContent(msg.getContent());
                    myAgent.send(respuesta);
                } else {
                    // Sin mensajes: suspende este comportamiento hasta que llegue uno nuevo
                    block();
                }
            }
        });
    }
}