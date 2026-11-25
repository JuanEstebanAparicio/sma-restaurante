package sma.restaurante.hito0;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.UUID;

public class AgenteEmisor extends Agent {

    private static final String DESTINATARIO = "cocina";
    private static final String ONTOLOGIA = "Restaurante";

    @Override
    protected void setup() {

        System.out.println(
                getLocalName() + " iniciado correctamente"
        );

        // Enviar solicitud de preparación cada 10 segundos.
        addBehaviour(new TickerBehaviour(this, 10000) {

            @Override
            protected void onTick() {

                ACLMessage mensaje =
                        new ACLMessage(ACLMessage.REQUEST);

                mensaje.addReceiver(
                        new AID(DESTINATARIO, AID.ISLOCALNAME)
                );

                mensaje.setOntology(ONTOLOGIA);

                mensaje.setProtocol(
                        FIPANames.InteractionProtocol.FIPA_REQUEST
                );

                String identificador = UUID.randomUUID().toString();

                mensaje.setConversationId(
                        "comanda-" + identificador
                );

                mensaje.setReplyWith(
                        "solicitud-" + identificador
                );

                // En el seeder, pedido_9 ya está en estado ACEPTADO
                mensaje.setContent(
                        "(preparar pedido_9 lomo_de_cerdo_en_salsa combo_limonada_patacones)"
                );

                myAgent.send(mensaje);

                System.out.println(
                        getLocalName()
                                + ": REQUEST enviado a "
                                + DESTINATARIO
                                + " -> "
                                + mensaje.getContent()
                );
            }
        });

        // Escucha tanto las respuestas FIPA-REQUEST (AGREE, INFORM, REFUSE)
        // como las notificaciones asíncronas (pedido_listo, retraso_cocina).
        MessageTemplate plantilla = MessageTemplate.MatchOntology(ONTOLOGIA);

        addBehaviour(new CyclicBehaviour(this) {

            @Override
            public void action() {

                ACLMessage respuesta = myAgent.receive(plantilla);

                if (respuesta != null) {

                    System.out.println(
                            "\n========== RESPUESTA ACL =========="
                    );

                    System.out.println(
                            "Agente emisor: "
                                    + respuesta.getSender().getLocalName()
                    );

                    System.out.println(
                            "Performativa: "
                                    + ACLMessage.getPerformative(
                                    respuesta.getPerformative()
                            )
                    );

                    System.out.println(
                            "Conversación: "
                                    + respuesta.getConversationId()
                    );

                    System.out.println(
                            "Respuesta a: "
                                    + respuesta.getInReplyTo()
                    );

                    System.out.println(
                            "Contenido: "
                                    + respuesta.getContent()
                    );

                    System.out.println(
                            "===================================\n"
                    );

                } else {
                    block();
                }
            }
        });
    }

    @Override
    protected void takeDown() {

        System.out.println(
                getLocalName() + " finalizado"
        );
    }
}