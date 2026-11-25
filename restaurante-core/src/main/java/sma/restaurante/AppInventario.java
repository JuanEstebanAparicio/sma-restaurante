package sma.restaurante;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;

public class AppInventario {

    public static void main(String[] args)
            throws Exception {

        Runtime runtime =
                Runtime.instance();

        Profile profile =
                new ProfileImpl();

        AgentContainer container =
                runtime.createMainContainer(
                        profile
                );

        container.createNewAgent(
                "inventario",
                "sma.restaurante.agent.AgenteInventario",
                null
        ).start();
    }
}