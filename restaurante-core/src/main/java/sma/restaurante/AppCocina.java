package sma.restaurante;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;

public class AppCocina {

    public static void main(String[] args) throws Exception {

        Runtime runtime =
                Runtime.instance();

        Profile profile =
                new ProfileImpl();

        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.MAIN_PORT, "1099");
        profile.setParameter(Profile.CONTAINER_NAME, "Contenedor-Cocina");

        AgentContainer container =
                runtime.createAgentContainer(
                        profile
                );

        container.createNewAgent(
                "cocina",
                "sma.restaurante.agent.AgenteCocina",
                null
        ).start();
    }
}